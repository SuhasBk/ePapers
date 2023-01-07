package com.epapers.epapers.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class EpapersUser {
    @Id
    String username;
    String telegramId;
    String password;
    String email;
    Map<String, String> editions;
    String city;
    boolean isOAuth;
}
