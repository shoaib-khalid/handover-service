package com.kalsym.handoverservice.agent.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 *
 * @author z33Sh
 */
public class CustomField {

    @SerializedName("key")
    @Expose
    public String key;
    @SerializedName("value")
    @Expose
    public String value;
    @SerializedName("overwrite")
    @Expose
    public Boolean overwrite;

    /**
     * No args constructor for use in serialization
     *
     */
    public CustomField() {
    }

    /**
     *
     * @param value
     * @param overwrite
     * @param key
     */
    public CustomField(String key, String value, Boolean overwrite) {
        super();
        this.key = key;
        this.value = value;
        this.overwrite = overwrite;
    }

    /**
     *
     * @param value
     * @param overwrite
     * @param k
     */
    public CustomField(CustomFields k, String value, Boolean overwrite) {
        super();
        this.key = k.toString();
        this.value = value;
        this.overwrite = overwrite;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("key", key).append("value", value).append("overwrite", overwrite).toString();
    }
}
