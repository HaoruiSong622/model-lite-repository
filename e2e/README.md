# E2E 测试

对部署在 Kind 集群的 model-lite-repository 发送真实 HTTP 请求，验证接口功能、异步流程及产物（K8s Job / PVC 文件）。

## 前置条件

| 项 | 要求 |
|----|------|
| Kind 集群 | 已创建（默认名 `modellite`），应用镜像 `model-lite-repository:0.1.0-SNAPSHOT` 已 load |
| conda 环境 | `model-lite`（Python 3.12），已装 `e2e/requirements.txt` 依赖 |
| 宿主机 | WSL2 内核支持 nfs 模块（`setup-nfs-env.sh` 会 `modprobe`）|
| 工具 | kubectl / helm / kind / docker / tmux / curl |

conda 环境创建（仅首次）：
```bash
~/miniconda3/bin/conda create -n model-lite python=3.12 -y
~/miniconda3/envs/model-lite/bin/pip install -r e2e/requirements.txt
```

## 一键运行

在项目根目录执行：

```bash
# 1. 部署全部：环境前置 + NFS server + file-copier 镜像 + 应用 + port-forward
make -f e2e/Makefile e2e-setup

# 2. 跑测试
make -f e2e/Makefile e2e-test

# 3. 看报告（启动本地 web server，浏览器自动打开）
make -f e2e/Makefile e2e-report

# 4. 清理
make -f e2e/Makefile e2e-down
```

## 手动分步（调试时）

```bash
# 环境前置：modprobe nfs（WSL2 重启后需重跑）
bash e2e/test-infra/scripts/setup-nfs-env.sh

# 部署 NFS 测试源（nfs-server-svc:/models，预置假权重文件）
bash e2e/test-infra/scripts/deploy-nfs-server.sh

# 构建 file-copier 镜像（上传 Job 容器）+ kind load
bash e2e/test-infra/scripts/build-file-copier.sh

# helm 部署应用（配 file-copier image + pg15-kind 镜像避开 docker.io 拉取超时）
make -f e2e/Makefile e2e-app

# port-forward（tmux session pf，直接转发 app pod 绕过 service selector bug）
make -f e2e/Makefile e2e-portforward

# 跑测试
make -f e2e/Makefile e2e-test
```

## 跑单个用例 / 单个文件

```bash
cd e2e
E2E_BASE_URL=http://localhost:8080 ~/miniconda3/envs/model-lite/bin/python -m pytest tests/smoke/test_smoke.py -v
E2E_BASE_URL=http://localhost:8080 ~/miniconda3/envs/model-lite/bin/python -m pytest tests/smoke/test_smoke.py::test_health_check -v
```

Markers：
- `@pytest.mark.contract` 单接口契约
- `@pytest.mark.e2e` 端到端流程
- `@pytest.mark.slow` 状态机边界
- `@pytest.mark.cifs_skip` CIFS 源（暂跳过）

按 marker 跑：`pytest -m contract`、`pytest -m "e2e and not slow"`

## 测试报告

报告用 **allure** 生成。`make e2e-report` 启动本地 web server，**每次随机分配一个可用端口**（命令输出会打印实际地址，如 `Server started at http://127.0.0.1:43740`），并尝试自动打开浏览器。从命令输出复制地址访问即可，不要假设固定端口。

### 报告内容（每个用例）

| 内容 | 来源 | 说明 |
|------|------|------|
| **用例名称** | allure 默认 | 函数名 + 参数化 |
| **API 请求历史** | `conftest._attach_report` 自动 attach | 每个请求：`[n] METHOD URL` + `Request body` + `Response [status]: body` |
| **k8s 观测** | `helpers/k8s.py` 调用时 attach | `k8s.get_job` → Job JSON；`k8s.list_pvc_files` → `$ ls -la /data/weights/...` 输出；`k8s.pod_logs` → pod 日志 |
| **应用 pod 日志** | 失败时自动 attach | 用例失败时抓 app pod 最近 100 行日志 |

所有 attachment 在 allure 报告的用例详情页「Attachments」区展示。请求历史和 k8s 观测是文本/JSON attachment，可直接在浏览器查看。

### 重新看报告（不重跑测试）

报告数据在 `e2e/allure-results/`。随时 `make e2e-report` 重新启动 web server 查看。

## 环境变量（可覆盖默认值）

| 变量 | 默认 | 说明 |
|------|------|------|
| `E2E_BASE_URL` | `http://localhost:8080` | 应用 base url |
| `E2E_NAMESPACE` | `modellite-dev` | 应用 + NFS server 所在 namespace |
| `E2E_PORT` | `8080` | port-forward 本地端口 |
| `PG_HOST/PG_PORT/PG_DB/PG_USER/PG_PASSWORD` | localhost/5432/modellite/modellite/changeme | DB 连接（需 port-forward 5432 或直连）|

## 已固化的部署坑（Makefile 已处理）

| 坑 | Makefile 解法 |
|----|---------------|
| helm affinity 排斥 control-plane（单节点 Pending）| 已删除 helm chart 的 affinity 逻辑 |
| `postgres:15` 镜像 docker.io 拉取超时 | `e2e-app` 用节点已有的 `pg15-kind:latest` |
| 应用 service selector 匹配到 postgresql pod（port-forward service 报错）| `e2e-portforward` 直接转发 app pod（`component!=postgresql` 过滤）|
| `kubectl port-forward &` 后台进程 bash 卡住 | 用 tmux session `pf` 跑 port-forward |
| WSL2 内核 nfs 模块未加载 | `e2e-env` 跑 `setup-nfs-env.sh` modprobe |

## 目录结构

```
e2e/
├── Makefile                    # e2e-setup/test/report/down
├── pytest.ini                  # markers + pythonpath
├── requirements.txt
├── conftest.py                 # fixtures + 自动 attach API 历史/失败日志
├── helpers/                    # client / k8s / db / waiters / assertions / error_codes
├── factories/builders.py       # 请求体构造
├── test-infra/                 # NFS server + file-copier 镜像 + 脚本
├── tests/smoke/                # 冒烟用例
└── allure-results/             # 报告数据（gitignore）
```

## 写新用例

```python
import pytest
from helpers.assertions import assert_success
from factories.builders import ModelBuilder

@pytest.mark.e2e
def test_create_and_get_model(client, k8s):
    resp = client.create_model(ModelBuilder().build())
    body = assert_success(resp)
    model_id = body["data"]["id"]

    resp = client.get_model(model_id)
    assert_success(resp)
    # k8s 观测会自动 attach 到报告
    # files = k8s.list_pvc_files(model_id, version_id)
```

client 请求历史和 k8s 观测无需手动 attach——conftest 和 helper 自动处理。
