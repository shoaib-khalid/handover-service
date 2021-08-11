package com.kalsym.handoverservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document(collection = "users")
@ToString
public class User implements Serializable {
    @Id
    public String id;
    public Date createdAt;
    public String avatarOrigin;
    public String name;
    public String username;
    public String status;
    public String statusDefault;
    public int utcOffset;
    public boolean active;
    public String type;
    public Date _updatedAt;
    public List<String> roles;
    public Object avatarETag;
}

//class CreatedAt{
//    public Date date;
//}
//
//class UpdatedAt{
//    public Date date;
//}

