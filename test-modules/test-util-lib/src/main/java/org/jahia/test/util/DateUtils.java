package org.jahia.test.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    private DateUtils() {
        // utility class
    }

    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
