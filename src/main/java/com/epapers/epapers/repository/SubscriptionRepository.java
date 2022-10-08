package com.epapers.epapers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.epapers.epapers.model.EpapersSubscription;

@Repository
public interface SubscriptionRepository extends MongoRepository<EpapersSubscription, String> {}
