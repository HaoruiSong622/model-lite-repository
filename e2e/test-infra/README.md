# NFS 测试基础设施 (test-infra)

为 E2E 测试提供真实的 NFS 上传源，用于验证模型仓库的 NFS 权重上传全流程。

## 架构

```
Kind 集群 (modellite-dev 命名空间)
├── model-lite-repository (应用 pod，挂载 model-weights PVC)      ← 被测系统
├── upload-<taskId> Job (上传时由应用创建)                          ← 被测系统
│     └─ file-copier 容器: rsync 从 NFS 源 → model-weights PVC
└── e2e-nfs-server pod (privileged, 共享 /exports/models)          ← 本测试基础设施
      └─ Service: nfs-server-svc (ClusterIP, 2049/111)
            └─ 预置假文件: fake-1.safetensors, fake-2.bin, readme.txt
```

应用创建上传任务时填：
- `sourceType = NFS`
- `nfsServer = nfs-server-svc`
- `nfsPath = /models`

## 前置条件

- Kind 集群已创建（默认名 `modellite`）
- 宿主机能执行 `modprobe`（WSL2 需要，加载 nfs 内核模块）
- Docker、kind、kubectl 已配置

## 快速使用

```bash
# 1. 环境前置（每次 WSL2 重启后执行一次）
bash e2e/test-infra/scripts/setup-nfs-env.sh

# 2. 部署 NFS server（构建镜像 + load + apply + DNS 修复 + 验证）
bash e2e/test-infra/scripts/deploy-nfs-server.sh

# 3. 跑 E2E 测试（后续步骤，client 用 nfs-server-svc:/models 作为上传源）
# ...

# 4. 清理
bash e2e/test-infra/scripts/cleanup-nfs-server.sh
```

## 配置参数（环境变量）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `KIND_CLUSTER_NAME` | `modellite` | Kind 集群名 |
| `NAMESPACE` | `modellite-dev` | NFS server 部署的 namespace（与应用同，便于短名解析）|
| `IMAGE_NAME` | `e2e-nfs-server` | 镜像名 |
| `IMAGE_TAG` | `v0.1.0` | 镜像 tag（需与 nfs-server.yaml 一致）|

示例：部署到独立 namespace
```bash
NAMESPACE=nfs-test-infra bash e2e/test-infra/scripts/deploy-nfs-server.sh
# 注意：独立 namespace 时，应用 Job 需用 FQDN nfs-server-svc.nfs-test-infra.svc.cluster.local
# 且需在节点 /etc/hosts 加该 FQDN 映射（deploy 脚本默认只加短名）
```

## 文件清单

```
test-infra/
├── README.md                  # 本文档
├── nfs-server/
│   ├── Dockerfile             # NFS server 镜像（alpine + nfs-utils + rpcbind + e2fsprogs）
│   └── entrypoint.sh          # 启动脚本（ext4 loop mount + rpcbind/nfsd/mountd + 预置假文件）
├── nfs-server.yaml            # Deployment + Service
└── scripts/
    ├── setup-nfs-env.sh       # 环境前置：modprobe + 验证 kind 节点 nfs-common
    ├── deploy-nfs-server.sh   # 构建+load+apply+等ready+修DNS+验证
    └── cleanup-nfs-server.sh  # 删 Deployment/Service + 清 /etc/hosts
```

## 已知坑与解法（穿刺验证）

| 坑 | 根因 | 解法 |
|----|------|------|
| WSL2 内核默认无 nfs 模块 | 模块未加载 | `setup-nfs-env.sh` 执行 `modprobe nfs nfsd` |
| overlayfs 不支持 NFS export | 容器可写层是 overlayfs | entrypoint 里 `dd + mkfs.ext4 + mount -o loop` 用 ext4 |
| mountd (100005) 没注册 | 只启动 nfsd 不够 | entrypoint 加 `rpc.mountd` |
| rpcbind 标准 daemon 模式异常 | 容器无 init 系统 | entrypoint 用 `rpcbind -d &`（debug/foreground 后台）|
| **kubelet 解析 nfs-server-svc 失败** | kind 节点 resolv.conf 指向 Docker DNS，不通 CoreDNS | `deploy-nfs-server.sh` 在节点 /etc/hosts 加 ClusterIP 映射 |
| pod 启动需时间，测太早看到中间态 | ext4 mkfs + 服务启动 ~10s | deploy 脚本用 `kubectl rollout status` 等 ready |

## WSL2 特有注意事项

1. **modprobe 非持久**：WSL2 重启后 nfs 模块卸载，需重新跑 `setup-nfs-env.sh`。
2. **setns 限制**：WSL2+containerd 下 `kubectl exec` 进 **privileged** pod 会失败（`setns: exit status 1`）。验证 PVC 文件时 `kubectl exec` 进**应用 pod**（非特权）查 `/data/weights/...`，不要 exec 进 NFS server pod 调试。NFS server 的健康度靠 `rpcinfo` 或 client 挂载结果判断。
3. **镜像构建加速**：Dockerfile 用阿里云 alpine 镜像源（`mirrors.aliyun.com`）加速 apk。非国内网络可去掉 `sed` 那行用官方源。

## NFS server 内部逻辑（entrypoint.sh）

1. `dd + mkfs.ext4 + mount -o loop` —— 创建 ext4 文件镜像并挂载到 /exports（overlayfs 不支持 NFS export）
2. 预置假文件到 /exports/models：`fake-1.safetensors` / `fake-2.bin`（白名单内）/ `readme.txt`（白名单外，测校验失败路径）
3. 配置 /etc/exports：`/exports` 设 `fsid=0`（NFSv4 伪根）+ `/exports/models` 子导出
4. `rpcbind -d &` → `rpc.nfsd 8` → `rpc.mountd` → `exportfs -rav`
5. `exec sleep infinity` 保持前台

client/Job 挂载用 NFSv4：`server=nfs-server-svc, path=/models`（相对 fsid=0 伪根）。
