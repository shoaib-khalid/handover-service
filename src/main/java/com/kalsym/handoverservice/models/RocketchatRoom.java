package com.kalsym.handoverservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author z33Sh
 */
@Getter
@Setter
@Document(collection = "rocketchat_room")
@ToString
public class RocketchatRoom {

    @Id
    public String id;
    public String fname;
}
