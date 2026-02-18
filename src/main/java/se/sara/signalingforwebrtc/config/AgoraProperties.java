package se.sara.signalingforwebrtc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agora.app")
public class AgoraProperties {
    
    private String id;
    private String certificate;
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCertificate() {
        return certificate;
    }
    
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
