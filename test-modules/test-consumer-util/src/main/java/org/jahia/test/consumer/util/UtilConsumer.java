package org.jahia.test.consumer.util;

import org.jahia.test.util.DateUtils;
import org.jahia.test.util.collections.ListUtils;
import org.jahia.test.util.io.IOUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class UtilConsumer {

    public String formatCurrentDate() {
        return DateUtils.formatDate(new Date());
    }

    public <T> List<T> getSafeList(List<T> list) {
        return ListUtils.safeList(list);
    }

    public void closeStream(InputStream is) {
        IOUtils.closeQuietly(is);
    }
}

