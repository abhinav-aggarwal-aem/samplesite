package com.samplesite.core.services.impl;

import com.samplesite.core.services.ResourceResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ReplicationPathsManagerImplTest {

    AemContext aemContext = new AemContextBuilder().build();

    @Mock
    ResourceResolverUtil resourceResolverUtil;

    ReplicationPathsManagerImpl serviceUnderTest;

    @BeforeEach
    void setUp() {
        when(resourceResolverUtil.getResourceResolver()).thenReturn(aemContext.resourceResolver());
        aemContext.registerService(ResourceResolverUtil.class, resourceResolverUtil);
    }

    @Test
    void executeOperation1() {
        aemContext.create().resource("/etc/custom-replication-paths");
        serviceUnderTest = aemContext.registerInjectActivateService(new ReplicationPathsManagerImpl());
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample3.html", "delete"));
    }

    @Test
    void executeOperation2() {
        serviceUnderTest = aemContext.registerInjectActivateService(new ReplicationPathsManagerImpl());
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample3.html", "delete"));
    }

    @Test
    void executeOperation3() {
        aemContext.create().resource("/etc/custom-replication-paths", "paths", "/content/sample3.html");
        serviceUnderTest = aemContext.registerInjectActivateService(new ReplicationPathsManagerImpl());
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "add"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample1.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample2.html", "delete"));
        assertEquals(Boolean.TRUE, serviceUnderTest.executeOperation("/content/sample3.html", "delete"));
    }

    @Test
    void containsPath() {
        serviceUnderTest = aemContext.registerInjectActivateService(new ReplicationPathsManagerImpl());
        assertEquals(Boolean.FALSE, serviceUnderTest.containsPath("/content/sample1.html"));
        serviceUnderTest.executeOperation("/content/sample1.html", "add");
        assertEquals(Boolean.TRUE, serviceUnderTest.containsPath("/content/sample1.html"));
        serviceUnderTest.executeOperation("/content/sample1.html", "delete");
        assertEquals(Boolean.FALSE, serviceUnderTest.containsPath("/content/sample1.html"));
    }
}