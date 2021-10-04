package com.samplesite.core.servlets;

import com.day.cq.replication.Replicator;
import com.samplesite.core.services.ReplicationPathsManager;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.osgi.service.event.EventAdmin;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandServletTest {

    AemContext aemContext = new AemContextBuilder().build();

    @Mock
    ReplicationPathsManager replicationPathsManager;

    @Mock
    Replicator replicator;

    @Mock
    EventAdmin eventAdmin;

    CommandServlet serviceUnderTest;

    @BeforeEach
    void setUp() {

        when(replicationPathsManager.executeOperation(any(), any())).thenReturn(true);
        aemContext.registerService(ReplicationPathsManager.class, replicationPathsManager);

        aemContext.registerService(Replicator.class, replicator);
        aemContext.registerService(EventAdmin.class, eventAdmin);

        serviceUnderTest = aemContext.registerInjectActivateService(new CommandServlet());


    }

    @Test
    void doPost1() throws ServletException, IOException {
        when(replicationPathsManager.containsPath("/content/sample1.html")).thenReturn(true);
        MockSlingHttpServletRequest request = Mockito.spy(aemContext.request());
        MockSlingHttpServletResponse response = aemContext.response();
        when(request.getParameterValues("path")).thenReturn(new String[]{"/content/sample1.html"});
        when(request.getParameter("cmd")).thenReturn("Activate");
        when(request.getParameter("version")).thenReturn("1.0");
        serviceUnderTest.doPost(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("Cannot replicate this page as it is already in workflow\n", response.getStatusMessage());
    }

    @Test
    void doPost2() throws ServletException, IOException {
        when(replicationPathsManager.containsPath("/content/sample1.html")).thenReturn(false);
        MockSlingHttpServletRequest request = Mockito.spy(aemContext.request());
        MockSlingHttpServletResponse response = aemContext.response();
        when(request.getParameterValues("path")).thenReturn(new String[]{"/content/sample1.html"});
        when(request.getParameter("cmd")).thenReturn("Activate");
        when(request.getParameter("version")).thenReturn("1.0");
        serviceUnderTest.doPost(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("Replication started for /content/sample1.html\n", response.getStatusMessage());
    }
}