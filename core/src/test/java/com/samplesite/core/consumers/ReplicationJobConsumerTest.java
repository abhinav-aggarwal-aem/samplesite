package com.samplesite.core.consumers;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.samplesite.core.services.ReplicationPathsManager;
import com.samplesite.core.services.ResourceResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.samplesite.core.constants.ApplicationConstants.CONTENT_PATH;
import static com.samplesite.core.constants.ApplicationConstants.REPLICATION_TYPE;
import static com.samplesite.core.constants.ApplicationConstants.VERSION_LABEL;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class ReplicationJobConsumerTest {

    AemContext aemContext = new AemContextBuilder().build();

    @Mock
    ResourceResolverUtil resourceResolverUtil;

    @Mock
    ReplicationPathsManager replicationPathsManager;

    @Mock
    Replicator replicator;

    ReplicationJobConsumer serviceUnderTest;

    @BeforeEach
    void setUp() {
        when(resourceResolverUtil.getResourceResolver()).thenReturn(aemContext.resourceResolver());
        aemContext.registerService(ResourceResolverUtil.class, resourceResolverUtil);

        when(replicationPathsManager.containsPath(any())).thenReturn(true);
        when(replicationPathsManager.executeOperation(any(), any())).thenReturn(true);
        aemContext.registerService(ReplicationPathsManager.class, replicationPathsManager);

        aemContext.registerService(Replicator.class, replicator);

        serviceUnderTest = aemContext.registerInjectActivateService(new ReplicationJobConsumer());
    }

    @Test
    void testProcess() {
        Job job = mock(Job.class);
        when(job.getProperty(VERSION_LABEL, String.class)).thenReturn("1.0");
        when(job.getProperty(CONTENT_PATH, String.class)).thenReturn("/content/sample.html");
        when(job.getProperty(REPLICATION_TYPE, ReplicationActionType.class)).thenReturn(ReplicationActionType.ACTIVATE);
        assertEquals(JobConsumer.JobResult.OK, serviceUnderTest.process(job));
    }
}