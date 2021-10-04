package com.samplesite.core.consumers;


import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.samplesite.core.services.ReplicationPathsManager;
import com.samplesite.core.services.ResourceResolverUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

import static com.samplesite.core.constants.ApplicationConstants.CONTENT_PATH;
import static com.samplesite.core.constants.ApplicationConstants.DELETE;
import static com.samplesite.core.constants.ApplicationConstants.REPLICATION_TYPE;
import static com.samplesite.core.constants.ApplicationConstants.VERSION_LABEL;

/**
 * This class with consume the job topic and replicate the content on specified time.
 */
@Component(service = JobConsumer.class, immediate = true, property = {
        JobConsumer.PROPERTY_TOPICS + "=" + "sample/job/replication"
})
public class ReplicationJobConsumer implements JobConsumer {


    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The replicator.
     */
    @Reference
    protected Replicator replicator;

    /**
     * The resourceResolverUtil.
     */
    @Reference
    protected ResourceResolverUtil resourceResolverUtil;

    /**
     * The replicationPathsManager.
     */
    @Reference
    protected ReplicationPathsManager replicationPathsManager;


    /**
     * Method to do the specified processing at specified time (i.e. replicating the page).
     *
     * @param job - the job object.
     *
     * @return - JobResult
     */
    public JobResult process(final Job job) {
        logger.info("Event topic: {} with event id: {}", job.getTopic(), job.getId());
        ReplicationOptions replicationOptions = new ReplicationOptions();
        String versionLabel = job.getProperty(VERSION_LABEL, String.class);
        if (StringUtils.isNotEmpty(versionLabel)) {
            replicationOptions.setRevision(versionLabel);
        }
        String contentPath = job.getProperty(CONTENT_PATH, String.class);
        ReplicationActionType replicationActionType = job.getProperty(REPLICATION_TYPE, ReplicationActionType.class);
        if (StringUtils.isNotEmpty(contentPath) && replicationActionType != null) {
            try {
                this.replicator.replicate(resourceResolverUtil.getResourceResolver().adaptTo(Session.class), replicationActionType, contentPath, replicationOptions);
            } catch (ReplicationException e) {
                e.printStackTrace();
            }
        }
        replicationPathsManager.executeOperation(contentPath, DELETE);
        logger.info("Event id: {} completed", job.getId());
        return JobResult.OK;
    }
}