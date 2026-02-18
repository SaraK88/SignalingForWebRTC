package se.sara.signalingforwebrtc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sara.signalingforwebrtc.exception.TokenGenerationException;
import se.sara.signalingforwebrtc.util.TokenBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // Allows your JS to talk to this server
public class TokenController {

    @Value("${agora.app.id}")
    private String appId;
    
    @Value("${agora.app.certificate}")
    private String appCert;

    @GetMapping("/rtm-token")
    public ResponseEntity<Map<String, String>> getRtmToken(@RequestParam String uid) {
        try {
            validateUid(uid);
            
            // Expire in 24 hours
            int expirationInSeconds = 86400;

            String token = TokenBuilder.buildRtmToken(appId, appCert, uid, expirationInSeconds);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("uid", uid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new TokenGenerationException("Failed to generate RTM token for uid: " + uid, e);
        }
    }
    
    @GetMapping("/media-token")
    public ResponseEntity<Map<String, String>> getMediaToken(@RequestParam String channelName, @RequestParam String uid) {
        try {
            validateUid(uid);
            validateChannelName(channelName);
            
            // Expire in 24 hours
            int expirationInSeconds = 86400;

            String token = TokenBuilder.buildMediaToken(appId, appCert, channelName, uid, expirationInSeconds);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("channelName", channelName);
            response.put("uid", uid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new TokenGenerationException("Failed to generate media token for channel: " + channelName + ", uid: " + uid, e);
        }
    }
    
    @ExceptionHandler(TokenGenerationException.class)
    public ResponseEntity<Map<String, String>> handleTokenGenerationException(TokenGenerationException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Token generation failed");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid parameters");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    private void validateUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("UID cannot be null or empty");
        }
        if (uid.length() > 64) {
            throw new IllegalArgumentException("UID cannot exceed 64 characters");
        }
    }
    
    private void validateChannelName(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty");
        }
        if (channelName.length() > 64) {
            throw new IllegalArgumentException("Channel name cannot exceed 64 characters");
        }
    }
}
