package com.mrkirby153.kcuhc.discord.oauth;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class representing an oauth client
 */
public class OAuthClient {

    protected static final OkHttpClient httpClient = new OkHttpClient();

    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String AUTH_URL = "https://discord.com/api/oauth2/authorize";

    private static final Map<UUID, String> state = new ConcurrentHashMap<>();
    private static final Map<UUID, String> scopes = new ConcurrentHashMap<>();

    private final UUID playerUuid;
    private final String clientId;
    private final String clientSecret;

    private final String redirectUrl;

    public OAuthClient(UUID playerUuid, String clientId, String clientSecret,
        String redirectUrl) {
        this.playerUuid = playerUuid;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
    }

    /**
     * Gets the authentication URL
     *
     * @param scopes The scopes to pass
     *
     * @return The authorization URL
     */
    public String getAuthUrl(String responseType, String... scopes) {
        String state = UUID.randomUUID().toString();
        OAuthClient.state.put(this.playerUuid, state);
        String joinedScopes = String.join(" ", scopes);
        OAuthClient.scopes.put(this.playerUuid, joinedScopes);
        try {
            URIBuilder builder = new URIBuilder(AUTH_URL);
            builder.addParameter("response_type", responseType);
            builder.addParameter("client_id", clientId);
            builder.addParameter("scope", joinedScopes);
            builder.addParameter("state", state);
            builder.addParameter("redirect_uri", redirectUrl);
            return builder.build().toASCIIString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Takes an authentication code and turns it into a set of OAuth tokens
     *
     * @param state     The state retrieved from the server
     * @param code      The code from the server
     * @param grantType The grant type to use
     *
     * @return The tokens
     */
    public OAuthTokens getTokens(String state, String code, String grantType) {
        String existingState = OAuthClient.state.remove(this.playerUuid);
        String scopes = OAuthClient.scopes.get(this.playerUuid);
        if (!existingState.equals(state)) {
            throw new OAuthException("State does not match");
        }
        FormBody body = new FormBody.Builder().add("client_id", this.clientId)
            .add("client_secret", this.clientSecret).add("grant_type", grantType).add("code", code)
            .add("redirect_uri", this.redirectUrl).add("scopes", scopes).build();
        try {
            return makeTokenRequest(body);
        } catch (IOException e) {
            throw new OAuthException("An unknown exception occurred", e);
        }
    }

    @NotNull
    private OAuthTokens makeTokenRequest(FormBody body) throws IOException {
        Request req = new Request.Builder().url(TOKEN_URL).post(body).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            ResponseBody respBody = resp.body();
            if (respBody == null) {
                throw new OAuthException("Body was null");
            }
            JSONObject bodyJson = new JSONObject(new JSONTokener(respBody.string()));
            return new OAuthTokens(bodyJson.getString("access_token"),
                bodyJson.getString("token_type"), bodyJson.getLong("expires_at"),
                bodyJson.getString("refresh_token"));
        }

    }

    public OAuthTokens refreshToken(String refreshToken) {
        FormBody body = new FormBody.Builder().add("client_id", this.clientId)
            .add("client_secret", this.clientSecret).add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken).build();
        try {
            return makeTokenRequest(body);
        } catch (IOException e) {
            throw new OAuthException("Unknown exception occurred", e);
        }
    }


    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public static class OAuthTokens {

        private final String accessToken;
        private final String tokenType;
        private final long expiresAt;
        private final String refreshToken;

        public OAuthTokens(String accessToken, String tokenType, long expiresAt,
            String refreshToken) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresAt = expiresAt;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
