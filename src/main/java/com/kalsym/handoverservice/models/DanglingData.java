package com.kalsym.handoverservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author z33Sh
 */
@Getter
@Setter
@ToString
public class DanglingData {

    long timeInMillis;
    String callBackUrl;

    public DanglingData(long timeInMillis, String callBackUrl) {
        this.timeInMillis = timeInMillis;
        this.callBackUrl = callBackUrl;
    }
}
