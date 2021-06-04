package com.kalsym.handoverservice.agent.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 *
 * @author z33Sh
 */
public class Agent {

    @SerializedName("agentId")
    @Expose
    public String agentId;
    @SerializedName("username")
    @Expose
    public String username;

    /**
     *
     * @param agentId
     * @param username
     */
    public Agent(String agentId, String username) {
        //super();
        this.agentId = agentId;
        this.username = username;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("agentId", agentId).append("username", username).toString();
    }
}
