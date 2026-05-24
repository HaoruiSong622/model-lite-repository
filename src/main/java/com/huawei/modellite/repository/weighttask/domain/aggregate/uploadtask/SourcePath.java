package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public final class SourcePath {

    private static final int MAX_PATH_LENGTH = 1024;

    private final SourceType sourceType;
    private final String path;
    private final CifsCredentials credentials;

    public static SourcePath ofNfs(String nfsServer, String nfsPath) {
        String path = nfsServer + ":" + nfsPath;
        validatePathLength(path);
        return new SourcePath(SourceType.NFS, path, null);
    }

    public static SourcePath ofCifs(String server, String share, String username, String password) {
        if (username == null || password == null) {
            throw new ModelLiteException(ErrorCode.UPLOAD_CIFS_CREDENTIALS_REQUIRED,
                    "CIFS模式下username和password不能为空");
        }
        String path = "//" + server + "/" + share;
        validatePathLength(path);
        CifsCredentials credentials = new CifsCredentials(username, password);
        return new SourcePath(SourceType.CIFS, path, credentials);
    }

    public static SourcePath ofPvc(String pvcName, String internalPath) {
        String path = pvcName + ":" + internalPath;
        validatePathLength(path);
        return new SourcePath(SourceType.PVC, path, null);
    }

    private static void validatePathLength(String path) {
        if (path.length() > MAX_PATH_LENGTH) {
            throw new ModelLiteException(ErrorCode.UPLOAD_SOURCE_PATH_INVALID,
                    "路径长度不能超过" + MAX_PATH_LENGTH + "个字符");
        }
    }
}
