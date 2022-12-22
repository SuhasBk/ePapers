package com.epapers.epapers.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.repository.SubscriptionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubscriptionService {
    
    @Autowired
    SubscriptionRepository subscriptionRepository;

    public void addSubscription(EpapersSubscription subscription) {
        subscriptionRepository.save(subscription);
        log.info("Subscribed successfully - {}", subscription);
    }

    public boolean removeSubscription(String chatId) {
        boolean result = true;
        EpapersSubscription subscription = subscriptionRepository.findById(chatId).orElse(null);
        if(subscription == null || !subscription.getIsActive()) {
            log.info("Not subscribed to unsubscribe! - {}", chatId);
            result = false;
        } else {
            subscription.setIsActive(false);
            subscriptionRepository.save(subscription);
            log.info("Unsubscribed successfully - {}", subscription);
        }
        return result;
    }

    public List<EpapersSubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}
