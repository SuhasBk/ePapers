package com.epapers.epapers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.epapers.epapers.model.EpapersUser;

public interface UserRepository extends MongoRepository<EpapersUser, String> {}
