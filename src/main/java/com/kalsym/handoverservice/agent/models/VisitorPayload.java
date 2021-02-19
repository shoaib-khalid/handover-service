package com.kalsym.handoverservice.agent.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 *
 * @author z33Sh
 */
@Getter
@Setter
public class VisitorPayload {

    @SerializedName("visitor")
    @Expose
    public Visitor visitor;

    /**
     * No args constructor for use in serialization
     *
     */
    public VisitorPayload() {
    }

    /**
     *
     * @param visitor
     */
    public VisitorPayload(Visitor visitor) {
        super();
        this.visitor = visitor;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("visitor", visitor).toString();
    }
}
