package com.samplesite.core.services.impl;

import com.samplesite.core.constants.ApplicationConstants;
import com.samplesite.core.services.ResourceResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;


/**
 * This Service will provide utility methods related to resource Resolver.
 *
 */
@Component(service = ResourceResolverUtil.class, immediate = true)
public class ResourceResolverUtilImpl implements Serializable, ResourceResolverUtil {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 2880754138217889657L;

    /**
     * The Constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceResolverUtil.class);

    /**
     * The resource resolver factory.
     */
    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    /**
     * Gets the resource resolver.
     *
     * @return the resource resolver
     */
    @Override
    public ResourceResolver getResourceResolver() {
        LOGGER.debug("START OF getResourceResolver METHOD");
        ResourceResolver resourceResolver = null;
        try {
            final Map<String, Object> authInfo =
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
                            (Object) ApplicationConstants.RESOURCE_RESOLVER_SERVICE);
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException le) {
            LOGGER.error("Login Exception" + le.getMessage(), le);
        }
        LOGGER.debug("END OF getResourceResolver METHOD");
        return resourceResolver;
    }
}
