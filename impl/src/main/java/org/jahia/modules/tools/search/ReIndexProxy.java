/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.tools.search;

import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;

import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is a proxy to ensure module compatibility with all Jahia Core versions.
 * For versions < 8.2.1.0 the consistency check is always true.
 * FOr versions >= 8.2.1.0 the consistency check can be passed as a parameter.
 *
 * @author Jerome Blanchard
 */
public class ReIndexProxy {

    private static Boolean SUPPORT_CONSISTENCY_CHECK = null;

    public static boolean supportConsistencyCheck() {
        if (SUPPORT_CONSISTENCY_CHECK == null) {
            try {
                Object repository = ((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository();
                repository.getClass().getMethod("scheduleReindexing", String.class, boolean.class);
                SUPPORT_CONSISTENCY_CHECK = true;
            } catch (NoSuchMethodException e) {
                SUPPORT_CONSISTENCY_CHECK = false;
            }
        }
        return SUPPORT_CONSISTENCY_CHECK;
    }

    public static void scheduleReindexing(String workspace, boolean consistencyCheck)
            throws RepositoryException, InvocationTargetException, IllegalAccessException {
        Object repository = ((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository();
        try {
            Method newReindex = repository.getClass().getMethod("scheduleReindexing", String.class, boolean.class);
            newReindex.invoke(repository, workspace, consistencyCheck);
        } catch (NoSuchMethodException e) {
            ((JahiaRepositoryImpl) repository).scheduleReindexing(workspace);
        }
    }

    public static void scheduleReindexing(boolean consistencyCheck)
            throws RepositoryException, InvocationTargetException, IllegalAccessException {
        Object repository = ((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository();
        try {
            Method newReindex = repository.getClass().getMethod("scheduleReindexing", boolean.class);
            newReindex.invoke(repository, consistencyCheck);
        } catch (NoSuchMethodException e) {
            ((JahiaRepositoryImpl) repository).scheduleReindexing();
        }
    }
}
