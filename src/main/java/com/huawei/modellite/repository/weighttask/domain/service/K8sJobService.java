package com.huawei.modellite.repository.weighttask.domain.service;

import com.huawei.modellite.repository.weighttask.domain.dto.JobStatus;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;

/**
 * Service for managing Kubernetes Jobs for file upload tasks.
 */
public interface K8sJobService {

    /**
     * Creates a Kubernetes Job for the given upload specification.
     * Idempotent: if the job already exists, this method does nothing.
     *
     * @param spec the upload job specification
     */
    void createUploadJob(UploadJobSpec spec);

    /**
     * Deletes the Kubernetes Job for the given task ID.
     * Idempotent: if the job does not exist, this method does nothing.
     *
     * @param taskId the task ID
     */
    void deleteJob(String taskId);

    /**
     * Deletes the Kubernetes Job and all associated resources (ConfigMap, Secret)
     * for the given task ID.
     *
     * @param taskId the task ID
     */
    void deleteJobResources(String taskId);

    /**
     * Gets the current status of the Job for the given task ID.
     *
     * @param taskId the task ID
     * @return the job status
     */
    JobStatus getJobStatus(String taskId);

    /**
     * Gets the logs of the Job's pod for the given task ID.
     *
     * @param taskId   the task ID
     * @param tailLines the number of lines to tail from the end
     * @return the pod logs, or empty string if not available
     */
    String getJobPodLogs(String taskId, int tailLines);

    /**
     * Checks whether a Job exists for the given task ID.
     *
     * @param taskId the task ID
     * @return true if the job exists
     */
    boolean jobExists(String taskId);

    /**
     * Gets the name of the first pod owned by the Job for the given task ID.
     *
     * @param taskId the task ID
     * @return the pod name, or null if not found
     */
    String getJobPodName(String taskId);
}
