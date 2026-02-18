package se.sara.signalingforwebrtc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.sara.signalingforwebrtc.config.AgoraProperties;
import se.sara.signalingforwebrtc.dto.TokenResponse;

@Service
public class TokenService {
    
    @Autowired
    private AgoraProperties agoraProperties;
    
    public ResponseEntity<TokenResponse> generateRtmToken(String uid) {
        try {
            // TODO: Implement actual RTM token generation
            TokenResponse response = new TokenResponse("rtm-token-" + uid, uid, 3600);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    public ResponseEntity<TokenResponse> generateMediaToken(String channelName, String uid) {
        try {
            // TODO: Implement actual media token generation
            TokenResponse response = new TokenResponse("media-token-" + channelName + "-" + uid, uid, 3600);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
