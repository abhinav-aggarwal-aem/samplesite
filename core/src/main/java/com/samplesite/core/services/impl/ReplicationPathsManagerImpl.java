package com.samplesite.core.services.impl;


import com.samplesite.core.services.ReplicationPathsManager;
import com.samplesite.core.services.ResourceResolverUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component(service = ReplicationPathsManager.class, immediate = true)
public class ReplicationPathsManagerImpl implements ReplicationPathsManager {

    private List<String> replicationPaths;

    @Reference
    protected ResourceResolverUtil resourceResolverUtil;

    @Activate
    protected void activate() {
        replicationPaths = new ArrayList<>();
        initializePaths();
    }

    private void initializePaths() {
        ResourceResolver resourceResolver = resourceResolverUtil.getResourceResolver();
        String path = "/etc/custom-replication-paths";
        if (resourceResolver != null && resourceResolver.getResource(path) != null) {
            Resource res = resourceResolver.getResource(path);
            String[] crxPaths = res.getValueMap().get("paths", String[].class);
            if (crxPaths != null && crxPaths.length > 0) {
                this.replicationPaths = new ArrayList<>(Arrays.asList(crxPaths));
            }
        } else if (resourceResolver != null) {
            getOrCreateBaseResource(resourceResolver);
        }
    }

    @Override
    public synchronized Boolean executeOperation(String resPath, String operationType) {
        Boolean operationExecuted = Boolean.FALSE;
        String crxPath = "/etc/custom-replication-paths";
        ResourceResolver resourceResolver = resourceResolverUtil.getResourceResolver();
        Resource res = getOrCreateBaseResource(resourceResolver);
        switch (operationType) {
            case "add":
                if (res != null) {
                    List<String> crxpathList;
                    ModifiableValueMap modifiableValueMap = res.adaptTo(ModifiableValueMap.class);
                    String[] crxPaths = modifiableValueMap.get("paths", String[].class);
                    if (crxPaths != null && crxPaths.length > 0) {
                        crxpathList = Arrays.asList(crxPaths);
                    } else {
                        crxpathList = new ArrayList<>();
                    }
                    List<String> modifiableList = new ArrayList<>(crxpathList);
                    modifiableList.add(resPath);
                    modifiableValueMap.put("paths", modifiableList.toArray());
                    try {
                        resourceResolver.commit();
                        this.replicationPaths.add(resPath);
                        operationExecuted = true;
                    } catch (PersistenceException e) {
                        operationExecuted = false;
                        e.printStackTrace();
                    }
                }
                break;
            case "delete":
                if (res != null) {
                    List<String> crxpathList;
                    ModifiableValueMap modifiableValueMap = res.adaptTo(ModifiableValueMap.class);
                    String[] crxPaths = modifiableValueMap.get("paths", String[].class);
                    if (crxPaths != null && crxPaths.length > 0) {
                        crxpathList = Arrays.asList(crxPaths);
                    } else {
                        crxpathList = new ArrayList<>();
                    }
                    List<String> newCrxPathList = new ArrayList<>(crxpathList);
                    newCrxPathList.remove(resPath);
                    modifiableValueMap.put("paths", newCrxPathList.toArray(new String[0]));
                    try {
                        resourceResolver.commit();
                        this.replicationPaths.remove(resPath);
                        operationExecuted = true;
                    } catch (PersistenceException e) {
                        operationExecuted = false;
                        e.printStackTrace();
                    }
                }
                break;
        }
        return operationExecuted;
    }

    @Override
    public Boolean containsPath(String path) {
        return replicationPaths.contains(path);
    }

    private Resource getOrCreateBaseResource(ResourceResolver resourceResolver) {
        Resource createdResource = null;
        if (resourceResolver != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("jcr:primaryType", "nt:unstructured");
            try {
                createdResource = ResourceUtil.getOrCreateResource(resourceResolver, "/etc/custom-replication-paths", properties, "sling:Folder", true);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        }
        return createdResource;
    }
}
