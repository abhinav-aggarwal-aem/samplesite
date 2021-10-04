package com.samplesite.core.services.impl;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceResolverUtilImplTest {

    AemContext aemContext = new AemContextBuilder().build();

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    ResourceResolverUtilImpl serviceUnderTest;

    @BeforeEach
    void setUp() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(aemContext.resourceResolver());
        aemContext.registerService(ResourceResolverFactory.class, resourceResolverFactory);
        serviceUnderTest = aemContext.registerInjectActivateService(new ResourceResolverUtilImpl());
    }

    @Test
    void getResourceResolver() {
        assertNotNull(serviceUnderTest.getResourceResolver());
    }
}