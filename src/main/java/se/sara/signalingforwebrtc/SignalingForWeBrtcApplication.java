package se.sara.signalingforwebrtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SignalingForWeBrtcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalingForWeBrtcApplication.class, args);
    }

}
