package com.kalsym.handoverservice.agent.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 *
 * @author z33Sh
 */
@Getter
@Setter
public class Visitor {

    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("token")
    @Expose
    public String token;
    @SerializedName("phone")
    @Expose
    public String phone;
    @SerializedName("customFields")
    @Expose
    public List<CustomField> customFields = null;

    /**
     * No args constructor for use in serialization
     *
     */
    public Visitor() {
    }

    /**
     *
     * @param phone
     * @param customFields
     * @param name
     * @param token
     */
    public Visitor(String name, String token, String phone, List<CustomField> customFields) {
        super();
        this.name = name;
        this.token = token;
        this.phone = phone;
        this.customFields = customFields;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("token", token).append("phone", phone).append("customFields", customFields).toString();
    }
}
