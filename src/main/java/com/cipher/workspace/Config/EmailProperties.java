package com.cipher.workspace.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    private String from = "noreply@workspace.com";
    private String senderName = "Workspace";
    private String inviteUrl = "http://localhost:3000/accept-invite";
    private String loginUrl = "http://localhost:3000/login";
    private String resetPasswordUrl = "http://localhost:3000/reset-password";
    private int tokenExpirationHours = 48;
}
