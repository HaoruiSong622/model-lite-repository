package com.huawei.modellite.repository.weighttask.domain.dto;

import com.huawei.modellite.repository.common.enums.SourceType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Specification for creating an upload Kubernetes Job.
 */
@Getter
@Builder
public class UploadJobSpec {

    private final String taskId;
    private final String modelId;
    private final String versionId;
    private final SourceType sourceType;
    private final String sourcePath;
    private final String targetPvcName;
    private final String targetSubPath;
    private final String cifsUsername;
    private final String cifsPassword;
    private final String image;
    private final String namespace;
    private final List<String> allowedSuffixes;

    /**
     * Creates a spec for NFS source type.
     */
    public static UploadJobSpec ofNfs(String taskId, String modelId, String versionId,
                                       String sourcePath, String targetPvcName, String targetSubPath,
                                       String image, String namespace, List<String> allowedSuffixes) {
        return UploadJobSpec.builder()
                .taskId(taskId)
                .modelId(modelId)
                .versionId(versionId)
                .sourceType(SourceType.NFS)
                .sourcePath(sourcePath)
                .targetPvcName(targetPvcName)
                .targetSubPath(targetSubPath)
                .image(image)
                .namespace(namespace)
                .allowedSuffixes(allowedSuffixes)
                .build();
    }

    /**
     * Creates a spec for CIFS source type.
     */
    public static UploadJobSpec ofCifs(String taskId, String modelId, String versionId,
                                        String sourcePath, String targetPvcName, String targetSubPath,
                                        String cifsUsername, String cifsPassword,
                                        String image, String namespace, List<String> allowedSuffixes) {
        return UploadJobSpec.builder()
                .taskId(taskId)
                .modelId(modelId)
                .versionId(versionId)
                .sourceType(SourceType.CIFS)
                .sourcePath(sourcePath)
                .targetPvcName(targetPvcName)
                .targetSubPath(targetSubPath)
                .cifsUsername(cifsUsername)
                .cifsPassword(cifsPassword)
                .image(image)
                .namespace(namespace)
                .allowedSuffixes(allowedSuffixes)
                .build();
    }

    /**
     * Creates a spec for PVC source type.
     */
    public static UploadJobSpec ofPvc(String taskId, String modelId, String versionId,
                                       String sourcePath, String targetPvcName, String targetSubPath,
                                       String image, String namespace, List<String> allowedSuffixes) {
        return UploadJobSpec.builder()
                .taskId(taskId)
                .modelId(modelId)
                .versionId(versionId)
                .sourceType(SourceType.PVC)
                .sourcePath(sourcePath)
                .targetPvcName(targetPvcName)
                .targetSubPath(targetSubPath)
                .image(image)
                .namespace(namespace)
                .allowedSuffixes(allowedSuffixes)
                .build();
    }
}
