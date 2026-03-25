package org.jahia.test.consumer.mixed;

import org.jahia.test.core.CoreService;
import org.jahia.test.core.api.CoreApi;
import org.jahia.test.util.DateUtils;
import org.jahia.test.util.collections.ListUtils;

import java.util.Date;
import java.util.List;

public class MixedConsumer implements CoreApi {

    private CoreService coreService;

    public MixedConsumer() {
        this.coreService = new CoreService();
    }

    @Override
    public String getVersion() {
        return coreService.getName() + " - " + DateUtils.formatDate(new Date());
    }

    public <T> List<T> getSafeList(List<T> list) {
        return ListUtils.safeList(list);
    }
}

