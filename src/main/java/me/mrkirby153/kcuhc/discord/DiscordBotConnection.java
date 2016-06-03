package me.mrkirby153.kcuhc.discord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import me.mrkirby153.kcuhc.discord.commands.IsLinked;
import me.mrkirby153.kcuhc.discord.commands.NewTeam;
import me.mrkirby153.kcuhc.discord.commands.RemoveTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordBotConnection {

    private final String host;
    private final int port;

    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;

    private ExecutorService executor;

    private boolean connected = false;

    private long lastFailure = -1;

    public static DiscordBotConnection instance;

    public DiscordBotConnection(String host, int port) {
        this.host = host;
        this.port = port;
        executor = Executors.newCachedThreadPool();
        instance = this;
    }


    public void connect() {
        try {
            if (connected)
                return;
            socket = new Socket(host, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            connected = true;
        } catch (ConnectException e) {
            UHC.plugin.getLogger().severe("Could not connect to the discord bot!");
            connected = false;
        } catch (Exception e) {
            e.printStackTrace();
            UHC.plugin.getLogger().severe("An error occurred from the socket! Disconnecting");
            connected = false;
            try {
                socket.close();
            } catch (IOException e1) {
                UHC.plugin.getLogger().severe("An error occurred when closing the socket because of an error.");
            }
        }
    }

    public ByteArrayDataInput sendMessage(byte[] message) {
        return sendMessage(message, 1);
    }

    public ByteArrayDataInput sendMessage(byte[] message, int attempt) {
        if (!connected) {
            if (attempt > 5) {
                lastFailure = System.currentTimeMillis();
                UHC.plugin.getLogger().info("Failed to connect to the robot! Giving up!");
                return null;
            }
            if (System.currentTimeMillis() > lastFailure + 120000) {
                UHC.plugin.getLogger().info("Attempting to reconnect to the discord bot (Attempt " + (attempt++) + ")");
                connect();
                sendMessage(message, attempt);
            } else {
                UHC.plugin.getLogger().info("Not attempting reconnect because the last failure was less than two minutes ago!");
                return null;
            }
        }
        try{
            ByteBuffer buff = ByteBuffer.allocate(4);
            buff.order(ByteOrder.LITTLE_ENDIAN);
            buff.putInt(message.length);
            buff.rewind();
            outputStream.write(buff.array());
            outputStream.write(message);
            outputStream.flush();
            if(inputStream == null)
                return null;
            byte[] messageSizeBytes = new byte[4];
            if (inputStream.read(messageSizeBytes) <= 0) return null;
            ByteBuffer msgLenBuff = ByteBuffer.wrap(messageSizeBytes);
            msgLenBuff.order(ByteOrder.LITTLE_ENDIAN);
            msgLenBuff.rewind();
            byte[] response = new byte[msgLenBuff.getInt()];
            inputStream.read(response);
            ByteArrayDataInput in = ByteStreams.newDataInput(response);
            String command = in.readUTF();
            return in;
        } catch (Exception e){
            e.printStackTrace();
            connected = false;
            UHC.plugin.getLogger().severe("There was an error sending that message and we have disconnected from the robot!");
            try {
                socket.close();
            } catch (IOException e1) {
                UHC.plugin.getLogger().severe("There was an error closing the socket due to an error");
            }
        }
        return null;
    }


    public void processAsync(Runnable runnable, Callback callback) {
        executor.execute(() -> {
            runnable.run();
            if (callback != null)
                callback.call();
        });
    }

    public void deleteAllTeamChannels(Callback callback) {
        processAsync(() -> {
            for (UHCTeam team : TeamHandler.teams()) {
                new RemoveTeam(team.getName()).send();
            }
        }, callback);
    }

    public void createAllTeamChannels(Callback callback) {
        processAsync(() -> {
            for (UHCTeam team : TeamHandler.teams()) {
                new NewTeam(team.getName());
            }
        }, callback);
    }

    public void getAllLinkedPlayers(ValueCallback<HashMap<UUID, String>> callback) {
        processAsync(() -> {
            HashMap<UUID, String> results = new HashMap<>();
            Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).forEach(u -> {
                ByteArrayDataInput response = new IsLinked(u).send();
                if (response.readBoolean()) {
                    results.put(u, response.readUTF().replace("::", "\\:\\:") + "::" + response.readUTF().replace("::", "\\:\\:"));
                } else {
                    results.put(u, null);
                }
            });
            callback.call(results);
        });
    }

    public void processAsync(Runnable runnable) {
        processAsync(runnable, null);
    }

    public void shutdown() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
        UHC.discordHandler = null;
    }


    public interface Callback {
        void call();
    }

    public interface ValueCallback<V> {
        void call(V data);
    }
}
