package com.kalsym.handoverservice.repositories;

import com.kalsym.handoverservice.models.RocketchatRoom;
import com.kalsym.handoverservice.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    @Query(value = "{'username' : ?0, 'statusLivechat' : { $in : ['available']}}")
    public Optional<User> findByUsername(String username);

}
