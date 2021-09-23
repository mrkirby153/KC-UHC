package com.mrkirby153.kcuhc.discord.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.oauth.OAuthClient.OAuthTokens;
import com.mrkirby153.kcuhc.discord.oauth.dto.MinecraftUser;
import com.mrkirby153.kcuhc.discord.oauth.dto.SavedOAuthUser;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.IntegerSetting;
import com.mrkirby153.kcuhc.module.settings.StringSetting;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordOAuthModule extends UHCModule {

    private static final int OAUTH_RETRY_COUNT = 1;
    private static final Random random = new Random();

    private final UHC uhc;

    private final String oauthClientId;
    private final String oauthClientSecret;
    private final String redirectUri;
    private final String oauthDashboardUrl;
    private final Map<String, UUID> oauthCodes = new ConcurrentHashMap<>();
    private final Map<UUID, SavedOAuthUser> savedOAuthUserMap = new ConcurrentHashMap<>();
    private FileConfiguration oauthConfiguration = new YamlConfiguration();
    private File oauthConfigurationFile;
    /**
     * A list of servers (comma separated) that the user must be in. This will match any server
     * in the list. If left blank, access will not be enforced
     */
    private StringSetting accessServers = new StringSetting("");
    private IntegerSetting apiPort = new IntegerSetting(42069);

    @Inject
    public DiscordOAuthModule(UHC uhc) {
        super("Discord OAuth", "Discord oauth settings", Material.WRITABLE_BOOK);
        this.uhc = uhc;

        this.oauthClientId = uhc.getConfig().getString("discord.oauth.clientId");
        this.oauthClientSecret = uhc.getConfig().getString("discord.oauth.clientSecret");
        this.redirectUri = uhc.getConfig().getString("discord.oauth.redirectUri");
        this.oauthDashboardUrl = uhc.getConfig().getString("discord.oauth.dashboard", "<NOT SET>");
        this.oauthConfigurationFile = new File(uhc.getDataFolder(), "discord-oauth.yml");
        if (!this.oauthConfigurationFile.exists()) {
            try {
                this.oauthConfigurationFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String dataToJson(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception processing Json");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        uhc.getLogger().info("Starting login checks for " + event.getUniqueId());
        UUID incomingUuid = event.getUniqueId();
        AccessState state = checkServerAccess(incomingUuid);
        doAccessCheck(event, state, 0);
    }

    private void doAccessCheck(AsyncPlayerPreLoginEvent event, AccessState state, int retryCount) {
        uhc.getLogger().info("Access " + state + " for " + event.getUniqueId());
        switch (state) {
            case DENIED_MISSING_OAUTH:
                // Generate a code to link discord oauth to minecraft
                String chars = "0123456789";
                StringBuilder code = new StringBuilder();
                for (int i = 0; i < 8; i++) {
                    code.append(chars.charAt(random.nextInt(chars.length())));
                }
                this.oauthCodes.put(code.toString(), event.getUniqueId());
                event.setKickMessage(ChatColor.GOLD + "Hold up!\n\n" + ChatColor.WHITE
                    + "There's one more thing you need to do before you can join:\n\n Visit "
                    + ChatColor.GREEN
                    + oauthDashboardUrl + ChatColor.WHITE + " and enter " + ChatColor.GOLD + code
                    + ChatColor.WHITE + " when prompted");
                event.setLoginResult(Result.KICK_WHITELIST);
                break;
            case DENIED_OAUTH_UNAUTHORIZED:
                if (retryCount < OAUTH_RETRY_COUNT && refreshTokens(event.getUniqueId())) {
                    uhc.getLogger().info("Retrying access check for " + event.getUniqueId());
                    doAccessCheck(event, checkServerAccess(event.getUniqueId()), retryCount + 1);
                } else {
                    event.setKickMessage(
                        "Not Whitelisted: Could not retrieve a list of servers you are in");
                    event.setLoginResult(Result.KICK_WHITELIST);
                }
                break;
            case GRANTED:
                uhc.getLogger().info("Allowing login");
                break;
            case DENIED_MISSING_SERVER:
                event.setKickMessage(
                    "Not Whitelisted: You are not a member of the required Discord server");
                event.setLoginResult(Result.KICK_WHITELIST);
                break;
            case DENIED_UNKNOWN:
                event.setKickMessage("An unknown error occurred checking your access");
                event.setLoginResult(Result.KICK_WHITELIST);
                break;
        }
    }

    /**
     * Generates a new {@link OAuthClient}
     *
     * @param uuid The UUID to create the Oauth client for
     *
     * @return The OAuth client
     */
    public OAuthClient makeOauthClient(UUID uuid) {
        return new OAuthClient(uuid, this.oauthClientId, this.oauthClientSecret, this.redirectUri);
    }

    /**
     * Gets the OAuth tokens for a user
     *
     * @param uuid The user
     *
     * @return Their OAuth tokens, or null if they do not exist
     */
    public OAuthTokens getTokens(UUID uuid) {
        if (!this.oauthConfiguration.isConfigurationSection(uuid.toString())) {
            return null;
        }
        ConfigurationSection section = this.oauthConfiguration.getConfigurationSection(
            uuid.toString());
        if (section == null) {
            throw new IllegalStateException("This should never happen (Malformed config?)");
        }
        return new OAuthTokens(section.getString("accessToken"), section.getString("tokenType"),
            section.getLong("expiresAt"), section.getString("refreshToken"));
    }

    /**
     * Saves the tokens for a user
     *
     * @param uuid   The user
     * @param tokens The tokens to save
     */
    public void saveTokens(UUID uuid, OAuthTokens tokens) {
        ConfigurationSection section = this.oauthConfiguration.getConfigurationSection(
            uuid.toString());
        if (section == null) {
            section = this.oauthConfiguration.createSection(uuid.toString());
        }
        section.set("accessToken", tokens.getAccessToken());
        section.set("tokenType", tokens.getTokenType());
        section.set("expiresAt", tokens.getExpiresAt());
        section.set("refreshToken", tokens.getRefreshToken());
        flushConfiguration();
    }

    /**
     * Loads the configuration file
     */
    public void loadConfiguration() {
        try {
            oauthConfiguration.load(oauthConfigurationFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flushes configuration to disk
     */
    public void flushConfiguration() {
        try {
            oauthConfiguration.save(oauthConfigurationFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SavedOAuthUser getDiscordUser(UUID uuid) {
        SavedOAuthUser existingUser = savedOAuthUserMap.get(uuid);
        if (existingUser != null) {
            return existingUser;
        }
        OAuthTokens existingTokens = getTokens(uuid);
        if (existingTokens == null) {
            return null;
        }
        Request discordRequest = new Request.Builder().url("https://discord.com/api/v9/users/@me")
            .addHeader("Authorization",
                String.format("Bearer %s", existingTokens.getAccessToken())).build();
        try (Response resp = OAuthClient.httpClient.newCall(discordRequest).execute()) {
            if (resp.code() == 401) {
                return null;
            }
            ResponseBody body = resp.body();
            if (body == null) {
                return null;
            }
            JSONObject user = new JSONObject(new JSONTokener(body.string()));
            return new SavedOAuthUser(user.getString("username"), user.getString("discriminator"),
                user.getString("id"));
        } catch (IOException e) {
            return null;
        }
    }

    public AccessState checkServerAccess(UUID uuid) {
        if (this.accessServers.getValue().equals("")) {
            return AccessState.GRANTED;
        }

        List<String> requiredServers = Arrays.asList(accessServers.getValue().split(","));

        OAuthTokens existingTokens = getTokens(uuid);
        if (existingTokens == null) {
            return AccessState.DENIED_MISSING_OAUTH;
        }
        Request serverRequest = new Request.Builder().url(
                "https://discord.com/api/v9/users/@me/guilds")
            .addHeader("Authorization", String.format("Bearer %s", existingTokens.getAccessToken()))
            .header("Content-Type", "application/json").build();
        try (Response resp = OAuthClient.httpClient.newCall(serverRequest).execute()) {
            if (resp.code() == 401) {
                return AccessState.DENIED_OAUTH_UNAUTHORIZED; // Our token expired, or they revoked access
            }
            ResponseBody body = resp.body();
            if (body == null) {
                return AccessState.DENIED_UNKNOWN; // This should never happen
            }
            JSONArray servers = new JSONArray(new JSONTokener(body.string()));
            for (Object entry : servers) {
                if (entry instanceof JSONObject) {
                    JSONObject serverJson = (JSONObject) entry;
                    String serverId = serverJson.getString("id");
                    if (requiredServers.contains(serverId)) {
                        return AccessState.GRANTED;
                    }
                }
            }
            return AccessState.DENIED_MISSING_SERVER;
        } catch (IOException e) {
            e.printStackTrace();
            return AccessState.DENIED_UNKNOWN;
        }
    }

    /**
     * Refresh the oauth tokens for the suer
     *
     * @param uuid The user to refresh oauth tokens for
     *
     * @return If the refresh was successful
     */
    private boolean refreshTokens(UUID uuid) {
        OAuthTokens tokens = getTokens(uuid);
        if (tokens == null) {
            return true; // There are no tokens to refresh
        }
        try {
            OAuthTokens newTokens = makeOauthClient(uuid).refreshToken(tokens.getRefreshToken());
            saveTokens(uuid, newTokens);
            return true;
        } catch (OAuthException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onLoad() {
        this.loadConfiguration();
        Spark.port(this.apiPort.getValue());
        Spark.get("/api/ping", (request, response) -> "Pong!");
        Spark.get("/api/code/:code", (request, response) -> {
            String code = request.params("code");
            UUID uuid = this.oauthCodes.get(code);
            if (uuid == null) {
                response.status(404);
                return "Code not found";
            } else {
                return uuid.toString();
            }
        });
        Spark.get("/api/auth-url/:uuid", (request, response) -> {
            UUID uuid = UUID.fromString(request.params("uuid"));
            return makeOauthClient(uuid).getAuthUrl("code", "identify", "guilds");
        });
        Spark.post("/api/map/:uuid", (request, response) -> {
            UUID uuid = UUID.fromString(request.params("uuid"));
            try {
                JSONObject body = new JSONObject(new JSONTokener(request.body()));
                OAuthClient client = makeOauthClient(uuid);
                OAuthTokens tokens = client.getTokens(body.getString("state"),
                    body.getString("code"), "authorization_code");
                saveTokens(uuid, tokens);
                uhc.getLogger().info("Generated tokens for " + uuid);
                response.status(200);
                this.oauthCodes.entrySet().removeIf(e -> e.getValue().equals(uuid));
                this.savedOAuthUserMap.remove(uuid);
                return "Success";
            } catch (OAuthException e) {
                JSONObject err = new JSONObject();
                err.put("error", e.getMessage());
                response.status(400);
                return err.toString();
            }
        });
        Spark.get("/api/user/:uuid", (request, response) -> {
            SavedOAuthUser user = getDiscordUser(UUID.fromString(request.params("uuid")));
            if (user == null) {
                response.status(404);
                return "Not found";
            }
            response.header("Content-Type", "application/json");
            return dataToJson(user);
        });
        Spark.get("/api/uuid/:uuid", (request, response) -> {
            UUID uuid = UUID.fromString(request.params("uuid"));
            OfflinePlayer player = this.uhc.getServer().getOfflinePlayer(uuid);
            response.header("Content-Type", "application/json");
            return dataToJson(new MinecraftUser(player.getUniqueId().toString(), player.getName()));
        });
    }

    @Override
    public void onUnload() {
        Spark.stop();
    }
}
