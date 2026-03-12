package org.jahia.test.util.io;

import java.io.IOException;
import java.io.InputStream;

public class IOUtils {
    public static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}

