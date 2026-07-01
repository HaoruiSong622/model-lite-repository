# file-copier 镜像

上传 Job 容器镜像。应用创建上传任务时，`Fabric8K8sJobService` 提交一个 K8s Job 用此镜像执行权重拷贝。

## 规格（与应用代码对齐）

| 项 | 值 | 来源 |
|----|----|------|
| 镜像名 | `modellite/file-copier:latest` | `TaskReconciler.rebuildJob` 硬编码（L150）|
| 容器名 | `file-copier` | `Fabric8K8sJobService.CONTAINER_NAME` |
| 源挂载点 | `/source` | `SOURCE_MOUNT_PATH` |
| 目标挂载点 | `/target` | `TARGET_MOUNT_PATH` |
| 进度日志格式 | `PROGRESS:<percent>` | `TaskReconciler.parseAndReportProgress` 倒序找 `PROGRESS:` 行提取数字 |

## 环境变量（由应用 ConfigMap/Secret 通过 EnvFrom 注入）

| 变量 | 来源 | 说明 |
|------|------|------|
| `SOURCE_TYPE` | ConfigMap | `NFS` / `CIFS` / `PVC` |
| `SOURCE_PATH` | ConfigMap | NFS/PVC 时为 `/source`（挂载点）；CIFS 时为 `//server/share` 原始路径 |
| `TARGET_PATH` | ConfigMap | `/target` 或 `/target/<subpath>` |
| `ALLOWED_SUFFIXES` | ConfigMap | 逗号分隔，如 `.safetensors,.bin` |
| `CIFS_USERNAME` | Secret（仅 CIFS） | CIFS 认证用户名 |
| `CIFS_PASSWORD` | Secret（仅 CIFS） | CIFS 认证密码 |

## entrypoint 两阶段逻辑

1. **校验**：源目录存在 + 所有文件后缀在白名单 → 通过输出 `validated`；失败 `exit 1`（Job Failed → 应用 `onJobFailed`）
2. **拷贝**：`rsync -a` 逐文件拷贝，每完成一个输出 `PROGRESS:<percent>`（按文件数计百分比），最后输出 `PROGRESS:100` + `copy completed`，`exit 0`（Job Complete → 应用 `onJobCompleted`）

## 构建

```bash
bash e2e/test-infra/scripts/build-file-copier.sh
```

默认 `modellite/file-copier:latest`。可覆盖：
```bash
IMAGE_NAME=my-registry/file-copier IMAGE_TAG=v1 bash e2e/test-infra/scripts/build-file-copier.sh
```

## 验证（已通过）

probe Job 复刻应用 `buildJob` 结构（ConfigMap EnvFrom + NFS source volume + target volume），挂载 `nfs-server-svc:/models`，日志输出：
```
validated
PROGRESS:33
PROGRESS:66
PROGRESS:100
copy completed
```
Job 3 秒 Complete，进度格式与 `TaskReconciler.parseAndReportProgress` 完全对齐。

## CIFS 说明（E2E 暂不测）

CIFS 分支在容器内 `mount -t cifs`，需要：
- 镜像含 `cifs-utils`（已装）
- Job spec 给 `SYS_ADMIN` capability（**应用 `buildJob` 当前未配置**，CIFS 实际会失败）

E2E 矩阵对 CIFS 标 `@cifs_skip` 暂跳过。待应用补 `securityContext.capabilities.SYS_ADMIN` 后再启用。

## 与应用配置的关系

应用 `weight-import.job.image`（默认 `file-copier:dev-placeholder`）需指向本镜像。建议 helm 部署时：
```bash
helm install model-lite-repository ./helm -n modellite-dev \
  --set weightImport.job.image=modellite/file-copier:latest
```
注意：`TaskReconciler.rebuildJob` 硬编码 `modellite/file-copier:latest`，暂停/恢复重建 Job 时用此名，保持一致。
