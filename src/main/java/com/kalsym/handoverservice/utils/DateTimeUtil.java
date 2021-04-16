package com.kalsym.handoverservice.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sarosh
 */
public class DateTimeUtil {

    private final static Logger LOG = LoggerFactory.getLogger("application");

    /**
     * *
     * Generate current timestamp string with format 'yyyy-MM-dd HH:mm:ss'
     *
     * @return
     */
    public static String currentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date currentDate = new Date();
        String currentTimeStamp = sdf.format(currentDate);
        return currentTimeStamp;
    }

    /**
     * *
     * Generate expiry time by adding seconds, hours or minutes with format
     * 'yyyy-MM-dd HH:mm:ss'
     *
     * @param seconds
     * @return
     */
    public static String expiryTimestamp(int seconds) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.SECOND, seconds);
        Date expiryDate = c.getTime();
        return dateFormat.format(expiryDate);
    }

    /**
     * Returns time difference in minutes from #actualTimeInMilli and current
     * time
     *
     * @param actualTimeInMilli
     * @return time difference in minutes or 0 in case of any exception
     */
    public static int getTimeDifferenceInMinutesFromMilli(long actualTimeInMilli) {
        int timeDifferenceInMinutes = 0;
        try {

            // finding the time difference
            long msec = System.currentTimeMillis() - actualTimeInMilli;
            // converting it into seconds
            long sec = msec / 1000L;
            // converting it into minutes
            long minutes = sec / 60L;
            System.out.println(minutes + " minutes");
            timeDifferenceInMinutes = (int) minutes;
        } catch (Exception ex) {
            LOG.error("Exception getting time difference, using default 0", ex);
            timeDifferenceInMinutes = 0;
        }
        return timeDifferenceInMinutes;
    }
}
