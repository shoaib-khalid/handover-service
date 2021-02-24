package com.kalsym.handoverservice.agent.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 *
 * @author z33Sh
 */
public class Message {

    @SerializedName("token")
    @Expose
    public String token;
    @SerializedName("rid")
    @Expose
    public String rid;
    @SerializedName("msg")
    @Expose
    public String msg;

    /**
     *
     * @param token
     * @param rid
     * @param msg
     */
    public Message(String token, String rid, String msg) {
//        super();
        this.token = token;
        this.rid = rid;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("token", token).append("rid", rid).append("msg", msg).toString();
    }
}
