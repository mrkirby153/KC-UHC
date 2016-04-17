package me.mrkirby153.kcuhc.discord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

public class DiscordBotConnection{

    private final String host;
    private final int port;

    private InputStream inputStream;
    private OutputStream os;
    private Socket socket;

    private KeyPair ourKey;
    private PublicKey theirKey;

    private boolean connected = false;

    public DiscordBotConnection(String host, int port){
        this.host = host;
        this.port = port;
        ourKey = Cryptography.generateKeypair(2048);
    }


    public void connect(){
        try {
            socket = new Socket(host, port);
            inputStream = socket.getInputStream();
            os = socket.getOutputStream();
            // Perform the handshake
            // Write our key
            os.write(ourKey.getPublic().getEncoded());
            os.flush();
            // Wait for thiers
            byte[] theirKey = new byte[2048];
            inputStream.read(theirKey);
            this.theirKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(theirKey));
            connected = true;
            connected = true;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message){
        try {
            ByteBuffer buff = ByteBuffer.allocate(4);
            buff.order(ByteOrder.LITTLE_ENDIAN);
            byte[] encrypted = Cryptography.encrypt(ourKey.getPublic(), message);
            buff.putInt(encrypted.length);
            buff.rewind();
            os.write(buff.array());
            os.write(encrypted);
            os.flush();
            processResponse();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void processResponse(){
        try {
            if (inputStream == null)
                return;
            byte[] messageSizeBytes = new byte[4];
            if (inputStream.read(messageSizeBytes) <= 0) return;
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
                return;
            }
            ByteArrayDataInput in = ByteStreams.newDataInput(rawDecrypted);
            String command = in.readUTF();
            DiscordBotResponseEvent responseEvent = new DiscordBotResponseEvent(command, in);
            UHC.plugin.getServer().getPluginManager().callEvent(responseEvent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public static class DiscordBotResponseEvent extends Event {
        private static final HandlerList handlers = new HandlerList();

        private final String command;
        private final ByteArrayDataInput response;

        public DiscordBotResponseEvent(String command, ByteArrayDataInput response){
            this.command = command;
            this.response = response;
        }

        public String getCommand() {
            return command;
        }

        public ByteArrayDataInput getResponse() {
            return response;
        }

        public static HandlerList getHandlerList(){
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
}
