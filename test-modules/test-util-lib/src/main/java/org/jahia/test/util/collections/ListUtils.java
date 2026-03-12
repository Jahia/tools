package org.jahia.test.util.collections;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    public static <T> List<T> safeList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }
}

