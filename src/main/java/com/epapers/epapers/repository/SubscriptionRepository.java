package com.epapers.epapers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.epapers.epapers.model.EpapersSubscription;

public interface SubscriptionRepository extends MongoRepository<EpapersSubscription, String> {}
