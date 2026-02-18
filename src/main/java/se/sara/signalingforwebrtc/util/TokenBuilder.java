package se.sara.signalingforwebrtc.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TokenBuilder {
    
    private static final String VERSION = "006";
    
    public static String buildRtmToken(String appId, String appCertificate, String userId, int expirationTime) {
        try {
            // Create token
            AccessToken token = new AccessToken(appId, appCertificate, "", userId);
            token.addPrivilege(AccessToken.Privileges.kRtmLogin, expirationTime);
            return token.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build RTM token", e);
        }
    }
    
    public static String buildMediaToken(String appId, String appCertificate, String channelName, String uid, int expirationTime) {
        try {
            // Create token for media channel
            AccessToken token = new AccessToken(appId, appCertificate, channelName, uid);
            token.addPrivilege(AccessToken.Privileges.kJoinChannel, expirationTime);
            token.addPrivilege(AccessToken.Privileges.kPublishAudioStream, expirationTime);
            token.addPrivilege(AccessToken.Privileges.kPublishVideoStream, expirationTime);
            return token.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build media token", e);
        }
    }
    
    public static class AccessToken {
        public enum Privileges {
            kJoinChannel(1),
            kPublishAudioStream(2),
            kPublishVideoStream(3),
            kPublishDataStream(4),
            kRtmLogin(1000);
            
            public final int value;
            
            Privileges(int value) {
                this.value = value;
            }
        }
        
        private final String appId;
        private final String appCertificate;
        private final String channelName;
        private final String uid;
        private final Map<Privileges, Integer> privileges = new TreeMap<>();
        private int expireTimestamp;
        
        public AccessToken(String appId, String appCertificate, String channelName, String uid) {
            this.appId = appId;
            this.appCertificate = appCertificate;
            this.channelName = channelName;
            this.uid = uid;
        }
        
        public AccessToken(String appId, String appCertificate, String uid, int expireTimestamp) {
            this(appId, appCertificate, "", uid);
            this.expireTimestamp = expireTimestamp;
        }
        
        public void addPrivilege(Privileges privilege, int expireTimestamp) {
            privileges.put(privilege, expireTimestamp);
        }
        
        public String build() throws Exception {
            if (!isValidUUID(appId) || !isValidUUID(appCertificate)) {
                throw new IllegalArgumentException("Invalid appId or appCertificate");
            }
            
            // Build message
            ByteBuffer message = ByteBuffer.allocate(1024);
            message.putInt(privileges.size());
            
            for (Map.Entry<Privileges, Integer> entry : privileges.entrySet()) {
                message.putInt(entry.getKey().value);
                message.putInt(entry.getValue());
            }
            
            message.flip();
            byte[] messageBytes = new byte[message.remaining()];
            message.get(messageBytes);
            
            // Generate signature
            byte[] signature = generateSignature(appCertificate, appId, channelName, uid, messageBytes);
            
            // Build token
            ByteBuffer pack = ByteBuffer.allocate(1024);
            pack.put(signature);
            pack.putInt(crc32(channelName));
            pack.putInt(crc32(uid));
            pack.put(messageBytes);
            
            pack.flip();
            byte[] packBytes = new byte[pack.remaining()];
            pack.get(packBytes);
            
            return VERSION + appId + Base64.encodeBase64String(packBytes);
        }
        
        private byte[] generateSignature(String appCertificate, String appId, String channelName, 
                                       String uid, byte[] message) throws Exception {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appCertificate.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            
            ByteBuffer buffer = ByteBuffer.allocate(appId.length() + channelName.length() + uid.length() + message.length);
            buffer.put(appId.getBytes(StandardCharsets.UTF_8));
            buffer.put(channelName.getBytes(StandardCharsets.UTF_8));
            buffer.put(uid.getBytes(StandardCharsets.UTF_8));
            buffer.put(message);
            
            return hmacSha256.doFinal(buffer.array());
        }
        
        private boolean isValidUUID(String uuid) {
            if (uuid == null || uuid.length() != 32) return false;
            return uuid.matches("[a-fA-F0-9]{32}");
        }
        
        private int crc32(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
                return ByteBuffer.wrap(hash).getInt();
            } catch (NoSuchAlgorithmException e) {
                return input.hashCode();
            }
        }
    }
}
