package com.samplesite.core.services;


/**
 * This Service will manage the paths for the scheduled replication.
 */
public interface ReplicationPathsManager {

    /**
     * Method to add/delete path from the management queue.
     *
     * @param path - the path on which the operation has to be performed.
     * @param operationType - which type of operation has to be performed.
     *
     * @return - true, if the operations has successfully completed
     */
    Boolean executeOperation(String path, String operationType);

    /**
     * Method to check if the passed path  is under the scheduled replication or not.
     *
     * @param path - the path on which the check has to be performed.
     *
     * @return - true, if path is present in the queue.
     */
    Boolean containsPath(String path);
}
