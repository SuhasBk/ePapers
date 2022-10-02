package com.epapers.epapers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.epapers.epapers.model.EpapersUser;

@Repository
public interface UserRepository extends MongoRepository<EpapersUser, String> {}
