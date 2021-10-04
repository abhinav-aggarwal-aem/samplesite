package com.samplesite.core.services;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * This Service will provide utility methods related to resource Resolver.
 */
public interface ResourceResolverUtil {

    /**
     * Gets the resource resolver.
     *
     * @return the resource resolver
     */
    ResourceResolver getResourceResolver();

}
