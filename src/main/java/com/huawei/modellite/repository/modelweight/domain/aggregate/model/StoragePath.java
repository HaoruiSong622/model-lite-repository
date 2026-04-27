package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;

import java.util.Objects;

public final class StoragePath {

    private final SourcePathType sourceType;
    private final String pvcName;
    private final String internalPath;
    private final String nfsServer;
    private final String nfsPath;

    private StoragePath(SourcePathType sourceType, String pvcName, String internalPath,
                        String nfsServer, String nfsPath) {
        this.sourceType = sourceType;
        this.pvcName = pvcName;
        this.internalPath = internalPath;
        this.nfsServer = nfsServer;
        this.nfsPath = nfsPath;
    }

    public static StoragePath ofPvc(String pvcName) {
        return ofPvc(pvcName, null);
    }

    public static StoragePath ofPvc(String pvcName, String internalPath) {
        if (pvcName == null || pvcName.isEmpty()) {
            throw new ModelLiteException(ErrorCode.STORAGE_PATH_PVC_NAME_REQUIRED,
                    "PVC模式下pvcName不能为空");
        }
        return new StoragePath(SourcePathType.PVC, pvcName, internalPath, null, null);
    }

    public static StoragePath ofNfs(String nfsServer, String nfsPath) {
        if (nfsServer == null || nfsPath == null) {
            throw new ModelLiteException(ErrorCode.STORAGE_PATH_NFS_REQUIRED,
                    "NFS模式下nfsServer和nfsPath不能为空");
        }
        return new StoragePath(SourcePathType.NFS, null, null, nfsServer, nfsPath);
    }

    public static StoragePath empty() {
        return new StoragePath(null, null, null, null, null);
    }

    public SourcePathType getSourceType() {
        return sourceType;
    }

    public String getPvcName() {
        return pvcName;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public String getNfsServer() {
        return nfsServer;
    }

    public String getNfsPath() {
        return nfsPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoragePath that = (StoragePath) o;
        return sourceType == that.sourceType &&
                Objects.equals(pvcName, that.pvcName) &&
                Objects.equals(internalPath, that.internalPath) &&
                Objects.equals(nfsServer, that.nfsServer) &&
                Objects.equals(nfsPath, that.nfsPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, pvcName, internalPath, nfsServer, nfsPath);
    }

    @Override
    public String toString() {
        return "StoragePath{" +
                "sourceType=" + sourceType +
                ", pvcName='" + pvcName + '\'' +
                ", internalPath='" + internalPath + '\'' +
                ", nfsServer='" + nfsServer + '\'' +
                ", nfsPath='" + nfsPath + '\'' +
                '}';
    }
}
