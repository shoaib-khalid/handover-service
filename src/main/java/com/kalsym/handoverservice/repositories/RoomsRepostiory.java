package com.kalsym.handoverservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.kalsym.handoverservice.models.*;
import org.springframework.data.mongodb.repository.Query;

/**
 *
 * @author z33Sh
 */
public interface RoomsRepostiory extends MongoRepository<RocketchatRoom, String> {

    @Query(value = "{'fname' : ?0}", delete = true)
    public RocketchatRoom deleteByFname(String fname);

}
