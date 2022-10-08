package com.epapers.epapers.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("subscriptions")
public class EpapersSubscription {
    @Id
    String chatId;
    Map<String, String> editions;
}
