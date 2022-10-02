package com.epapers.epapers.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.telegram.telegrambots.meta.api.objects.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class EpapersUser {
    @Id
    String chatId;
    User user;
    String edition;
    String location;
    String timeStamp;
    Integer count;
}
