package me.mrkirby153.kcuhc.discord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordBotConnection {

    private final String host;
    private final int port;

    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;

    private KeyPair ourKey;
    private PublicKey theirKey;
    private ExecutorService executor;

    private boolean connected = false;

    public DiscordBotConnection(String host, int port) {
        this.host = host;
        this.port = port;
        ourKey = Cryptography.generateKeypair(2048);
        executor = Executors.newCachedThreadPool();
    }


    public void connect() {
        try {
            if(connected)
                return;
            socket = new Socket(host, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            // Perform the handshake
            // Write our key
            outputStream.write(ourKey.getPublic().getEncoded());
            outputStream.flush();
            // Wait for thiers
            byte[] theirKey = new byte[2048];
            inputStream.read(theirKey);
            this.theirKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(theirKey));
            connected = true;
        } catch (Exception e) {
            e.printStackTrace();
            UHC.plugin.getLogger().severe("An error occurred from the socket! Disconnecting");
            connected = false;
            try{
                socket.close();
            } catch (IOException e1) {
                UHC.plugin.getLogger().severe("An error occurred when closing the socket because of an error.");
            }
        }
    }

    public ByteArrayDataInput sendMessage(byte[] message) {
        if(!connected)
            throw new IllegalStateException("Not connected to the discord bot!");
        try {
            ByteBuffer buff = ByteBuffer.allocate(4);
            buff.order(ByteOrder.LITTLE_ENDIAN);
            byte[] encrypted = Cryptography.encrypt(theirKey, message);
            buff.putInt(encrypted.length);
            buff.rewind();
            outputStream.write(buff.array());
            outputStream.write(encrypted);
            outputStream.flush();
            if (inputStream == null)
                return null;
            byte[] messageSizeBytes = new byte[4];
            if (inputStream.read(messageSizeBytes) <= 0) return null;
            ByteBuffer msgLenBuff = ByteBuffer.wrap(messageSizeBytes);
            msgLenBuff.order(ByteOrder.LITTLE_ENDIAN);
            msgLenBuff.rewind();
            byte[] encodedMessage = new byte[msgLenBuff.getInt()];
            inputStream.read(encodedMessage);
            ByteBuffer msgBuff = ByteBuffer.wrap(encodedMessage);
            msgBuff.rewind();
            byte[] rawDecrypted = Cryptography.decrypt(ourKey.getPrivate(), encodedMessage);
            if (rawDecrypted == null) {
                UHC.plugin.getLogger().info("Decryption failed!");
                return null;
            }
            ByteArrayDataInput in = ByteStreams.newDataInput(rawDecrypted);
            String command = in.readUTF();
            return in;
        } catch (Exception e) {
            e.printStackTrace();
            connected = false;
            UHC.plugin.getLogger().warning("Disconnected from robot!");
            try{
                socket.close();
            } catch (Exception e1){
                e.printStackTrace();
            }
        }
        return null;
    }


    public void processAsync(Runnable runnable, Callback callback){
        executor.execute(()->{
            runnable.run();
            if(callback != null)
            callback.call();
        });
    }

    public void deleteAllTeamChannels(Callback callback){
        processAsync(()->{
            for (UHCTeam team : TeamHandler.teams()) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(UHC.plugin.serverId());
                out.writeUTF("removeTeam");
                out.writeUTF(team.getName());
                UHC.discordHandler.sendMessage(out.toByteArray());
            }
        }, callback);
    }

    public void createAllTeamChannels(Callback callback){
        processAsync(()->{
            for (UHCTeam team : TeamHandler.teams()) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(UHC.plugin.serverId());
                out.writeUTF("newTeam");
                out.writeUTF(team.getName());
                UHC.discordHandler.sendMessage(out.toByteArray());
            }
        }, callback);
    }

    public void processAsync(Runnable runnable){
        processAsync(runnable, null);
    }

    public void shutdown(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
        UHC.discordHandler = null;
    }


    public static class DiscordBotResponseEvent extends Event {
        private static final HandlerList handlers = new HandlerList();

        private final String command;
        private final ByteArrayDataInput response;

        public DiscordBotResponseEvent(String command, ByteArrayDataInput response) {
            this.command = command;
            this.response = response;
        }

        public String getCommand() {
            return command;
        }

        public ByteArrayDataInput getResponse() {
            return response;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }

    private static class Cryptography {

        public static KeyPair generateKeypair(int bits) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits, RSAKeyGenParameterSpec.F4);
                kpg.initialize(spec);
                return kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static byte[] decrypt(PrivateKey privateKey, byte[] message) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                return cipher.doFinal(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public static byte[] encrypt(PublicKey publicKey, byte[] message) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                return cipher.doFinal(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public interface Callback{
        void call();
    }
}
