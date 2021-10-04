package com.samplesite.core.servlets;

import com.day.cq.replication.AccessDeniedException;
import com.day.cq.replication.PathNotFoundException;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.samplesite.core.services.ReplicationPathsManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static com.samplesite.core.constants.ApplicationConstants.SLING_SERVLET_METHOD;
import static com.samplesite.core.constants.ApplicationConstants.SLING_SERVLET_PATHS;

@Component(service = Servlet.class, immediate = true,
        property = {SLING_SERVLET_METHOD + HttpConstants.METHOD_POST, SLING_SERVLET_PATHS + "/bin/replicate"})
@ServiceRanking(60000)
public class CommandServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = -647584595694478227L;
    private final transient Logger logger;
    @Reference
    private transient Replicator replicator;

    @Reference
    protected ReplicationPathsManager replicationPathsManager;

    @Reference
    private transient EventAdmin eventAdmin;


    public CommandServlet() {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {
        if (this.isJson(request)) {
            response.setContentType("application/json");
        } else {
            response.setContentType("text/plain");
        }
        final String[] paths = request.getParameterValues("path");
        if (paths == null || paths.length == 0) {
            this.writeStatus((HttpServletResponse) response, "Error: path parameter is missing", 400, paths, this.isJson(request));
            return;
        }
        final String parameter = request.getParameter("batch");
        boolean batch = paths.length > 1;
        if (parameter != null && parameter.length() > 1) {
            batch = Boolean.valueOf(parameter);
        }
        final String actionParam = request.getParameter("cmd");
        final ReplicationActionType action = ReplicationActionType.fromName(actionParam);
        if (action == null) {
            this.writeStatus((HttpServletResponse) response, "Error: cmd contains unknown value: " + actionParam, 400, paths, this.isJson(request));
            return;
        }
        final Replicator localReplicator = this.replicator;
        if (localReplicator == null) {
            this.writeStatus((HttpServletResponse) response, "Error: Replicator service is not available", 400, paths, this.isJson(request));
            return;
        }
        final ReplicationOptions opts = new ReplicationOptions();
        opts.setRevision(request.getParameter("version"));
        final List<String> msgs = (List<String>) new ArrayList();
        final Session session = (Session) request.getResourceResolver().adaptTo((Class) Session.class);
        int status = 200;
        if (batch) {
            final String pathsString = Arrays.toString((Object[]) paths);
            String msg;
            try {
                localReplicator.replicate(session, action, paths, opts);
                msg = "Replication started for " + pathsString;
            } catch (PathNotFoundException pnfe) {
                msg = "Error: One of the following paths was not found: " + pathsString;
                status = 404;
            } catch (AccessDeniedException ade) {
                if (action.equals((Object) ReplicationActionType.ACTIVATE) || action.equals((Object) ReplicationActionType.DEACTIVATE)) {
                    for (final String path : paths) {
                        this.activateByWorkflow(path, request, action, session);
                    }
                    msg = "Warn: No rights to replicate some paths. Request for de/activation got issued for " + pathsString;
                    status = 403;
                } else {
                    msg = "Error: No rights to replicate one of " + pathsString;
                    status = 403;
                }
            } catch (Throwable e) {
                this.logger.error("Error during replication: " + e.getMessage(), e);
                msg = "Error: " + e.getLocalizedMessage() + " for paths " + pathsString;
                status = 400;
            }
            msgs.add(msg);
        } else {
            for (final String path2 : paths) {
                String msg2;
                try {
                    if (replicationPathsManager.containsPath(path2)) {
                        msg2 = "Cannot replicate this page as it is already in workflow";
                    } else {
                        localReplicator.replicate(session, action, path2, opts);
                        msg2 = "Replication started for " + path2;
                    }
                } catch (PathNotFoundException pnfe2) {
                    msg2 = "Error: Path not found: " + path2;
                    if (status == 200) {
                        status = 404;
                    }
                } catch (AccessDeniedException ade2) {
                    if (action.equals((Object) ReplicationActionType.ACTIVATE) || action.equals((Object) ReplicationActionType.DEACTIVATE)) {
                        this.activateByWorkflow(path2, request, action, session);
                        msg2 = "Warn: No rights to replicate. Request for de/activation got issued for " + path2;
                        if (status == 200) {
                            status = 403;
                        }
                    } else {
                        msg2 = "Error: No rights to replicate " + path2;
                        if (status == 200) {
                            status = 403;
                        }
                    }
                } catch (Throwable e2) {
                    this.logger.error("Error during replication: " + e2.getMessage(), e2);
                    msg2 = "Error: " + e2.getLocalizedMessage() + " for path " + path2;
                    if (status == 200) {
                        status = 400;
                    }
                }
                msgs.add(msg2);
            }
        }
        final StringBuilder msg3 = new StringBuilder();
        for (final String m : msgs) {
            msg3.append(m).append("\n");
        }
        this.writeStatus((HttpServletResponse) response, msg3.toString(), status, paths, this.isJson(request));
    }

    private void activateByWorkflow(final String path, final SlingHttpServletRequest request, final ReplicationActionType action, final Session session) {
        this.logger.debug(request.getRemoteUser() + " is not allowed to replicate this resource " + path + ". Issuing request for 'replication");
        final Dictionary<String, Object> properties = (Dictionary<String, Object>) new Hashtable();
        properties.put("path", (Object) path);
        properties.put("replicationType", (Object) action);
        properties.put("userId", (Object) session.getUserID());
        final Event event = new Event("com/day/cq/wcm/workflow/req/for/activation", (Dictionary) properties);
        this.eventAdmin.sendEvent(event);
    }

    private boolean isJson(final SlingHttpServletRequest request) {
        return "json".equals((Object) request.getRequestPathInfo().getExtension());
    }

    private void writeStatus(final HttpServletResponse response, final String message, final int status, final String[] paths, final boolean isJson) throws IOException {
        response.setStatus(status, message);
        this.createStatusResponse(status, message, paths).send(response, true);
    }

    private HtmlResponse createStatusResponse(final int status, final String message, final String[] paths) {
        final HtmlResponse res = new HtmlResponse();
        res.setStatus(status, message);
        if (paths != null && paths.length > 0) {
            res.setPath(paths[0]);
        }
        return res;
    }
}