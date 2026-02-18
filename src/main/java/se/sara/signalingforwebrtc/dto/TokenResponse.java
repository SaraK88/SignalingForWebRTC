package se.sara.signalingforwebrtc.dto;

public class TokenResponse {
    
    private String token;
    private String uid;
    private long expirationTime;
    
    public TokenResponse() {}
    
    public TokenResponse(String token, String uid, long expirationTime) {
        this.token = token;
        this.uid = uid;
        this.expirationTime = expirationTime;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }
}
