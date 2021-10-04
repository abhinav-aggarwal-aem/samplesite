package com.samplesite.core.workflow.process;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.collection.ResourceCollection;
import com.adobe.granite.workflow.collection.ResourceCollectionManager;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.wcm.workflow.process.ResourceCollectionHelper;
import com.samplesite.core.services.ReplicationPathsManager;
import com.samplesite.core.services.ResourceResolverUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(
        service = WorkflowProcess.class,
        property = {"process.label=Sample - ReplicatePageProcess"}
)
public class ReplicatePageProcess implements WorkflowProcess {

    private static final Logger logger = LoggerFactory.getLogger(ReplicatePageProcess.class);
    @Reference
    protected EventAdmin eventAdmin;
    @Reference
    protected ResourceCollectionManager rcManager;

    @Reference
    protected ResourceResolverUtil resourceResolverUtil;

    @Reference
    protected ReplicationPathsManager replicationPathsManager;

    @Reference
    protected JobManager jobManager;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args)
            throws WorkflowException {
        logger.debug("start of  execute method");
        Session replicationSession = null;
        ResourceResolver resourceResolver = resourceResolverUtil.getResourceResolver();
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            replicationSession = session;
            final WorkflowData data = workItem.getWorkflowData();
            String abstime = data.getMetaDataMap().get("absoluteTime", String.class);

            String path = null;
            final String type = data.getPayloadType();
            if (type.equals("JCR_PATH") && data.getPayload() != null) {
                final String payloadData = (String) data.getPayload();
                if (replicationSession.itemExists(payloadData)) {
                    path = payloadData;
                }
            } else if (data.getPayload() != null && type.equals("JCR_UUID")) {
                final Node node = replicationSession.getNodeByUUID((String) data.getPayload());
                path = node.getPath();
            }
            final MetaDataMap metaDataMap = data.getMetaDataMap();
            final Map<String, String> versionMap = (Map<String, String>) new HashMap();
            if (metaDataMap.containsKey("versions")) {
                final JSONObject versionJs = new JSONObject((String) data.getMetaDataMap().get("versions", (Class) String.class));
                final Iterator iterator = versionJs.keys();
                while (iterator.hasNext()) {
                    final String key = (String) iterator.next();
                    versionMap.put(key, String.valueOf(versionJs.get(key)));
                }
            }

            if (path != null) {
                final List<ResourceCollection> rcCollections = (List<ResourceCollection>) this.rcManager.getCollectionsForNode((Node) replicationSession.getItem(path));
                final List<String> paths = ResourceCollectionHelper.getPaths(path, rcCollections);
                for (final String aPath : paths) {
                    if (this.canReplicate(replicationSession, aPath)) {
                        ReplicationOptions opts = new ReplicationOptions();
                        final String versionLabel = this.getVersionLabel(aPath, versionMap);
                        if (this.replicationPathsManager.executeOperation(aPath, "add")) {
                            triggerJob(this.getReplicationType(), aPath, versionLabel, abstime);
                        }
                    } else {
                        ReplicatePageProcess.logger.debug(session.getUserID() + " is not allowed to replicate this page/asset " + aPath + ". Issuing request for 'replication");
                        final Dictionary<String, Object> properties = (Dictionary<String, Object>) new Hashtable();
                        properties.put("path", (Object) aPath);
                        properties.put("replicationType", (Object) this.getReplicationType());
                        properties.put("userId", (Object) session.getUserID());
                        final Event event = new Event("com/day/cq/wcm/workflow/req/for/activation", (Dictionary) properties);
                        this.eventAdmin.sendEvent(event);
                    }
                }
            } else {
                ReplicatePageProcess.logger.warn("Cannot activate page or asset because path is null for this workitem: " + workItem.toString());
            }
        } catch (RepositoryException e) {
            throw new WorkflowException((Throwable) e);
        } catch (JSONException e3) {
            throw new WorkflowException((Throwable) e3);
        } finally {
            if (replicationSession != null && replicationSession.isLive()) {
                replicationSession.logout();
                replicationSession = null;
            }
        }


    }

    /**
     * Method to trigger the sling job for the current path and action type.
     *
     * @param replicationType - the type of replication action
     *
     * @param aPath - path for which the sling job has to be created.
     * @param versionLabel - the version of page.
     * @param abstime - the absolute time(in milli seconds) on which the page has to be replicated.
     */
    private void triggerJob(ReplicationActionType replicationType, String aPath, String versionLabel, String abstime) {
        logger.debug("start of  triggerJob method with aPath: {}", aPath);
        JobBuilder jobBuilder = jobManager.createJob("sample/job/replication");
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("contentPath", aPath == null ? "" : aPath);
        props.put("versionLabel", versionLabel == null ? "" : versionLabel);
        props.put("replicationType", replicationType);
        jobBuilder.properties(props);
        JobBuilder.ScheduleBuilder scheduleBuilder = jobBuilder.schedule();
        if (StringUtils.isNoneBlank(abstime)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.parseLong(abstime));
            scheduleBuilder.at(calendar.getTime());
            logger.debug("starting job for path: {}", aPath);
            scheduleBuilder.add();
        }
        logger.debug("end of  triggerJob method with aPath: {}", aPath);
    }

    private ReplicationActionType getReplicationType() {
        return ReplicationActionType.ACTIVATE;
    }

    private ReplicationOptions prepareOptions(final ReplicationOptions opts) {
        return opts;
    }

    private String getVersionLabel(String path, final Map<String, String> versionMap) {
        if (StringUtils.isEmpty((CharSequence) path)) {
            return null;
        }
        if (versionMap.containsKey((Object) path)) {
            return (String) versionMap.get((Object) path);
        }
        if (!path.endsWith("/jcr:content")) {
            path += "/jcr:content";
        }
        return (String) versionMap.get((Object) path);
    }

    private boolean canReplicate(final Session session, final String path) throws AccessDeniedException {
        try {
            final AccessControlManager acMgr = session.getAccessControlManager();
            return acMgr.hasPrivileges(path, new Privilege[]{acMgr.privilegeFromName("{http://www.day.com/crx/1.0}replicate")});
        } catch (RepositoryException e) {
            return false;
        }
    }

}
