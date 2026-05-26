# Helm Deployment Guide

Deploy ModelLite Repository to an isolated Kubernetes namespace for development and testing.

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| `kubectl` | >= 1.24 | Cluster interaction |
| `helm` | >= 3.10 | Chart deployment |
| `docker` | >= 20.10 | Image build |
| Java 21 + Maven | — | Application build |

Ensure `kubectl` is configured and connected to your target cluster:

```bash
kubectl cluster-info
```

## Directory Structure

```
helm/
├── Chart.yaml              # Chart metadata
├── values.yaml             # Default configuration values
├── templates/
│   ├── _helpers.tpl        # Template helpers
│   ├── deployment.yaml     # Application Deployment
│   ├── service.yaml        # Service (ClusterIP / NodePort)
│   ├── configmap.yaml      # Spring Boot K8s config
│   ├── secret.yaml         # Database credentials
│   ├── serviceaccount.yaml # ServiceAccount
│   ├── postgresql-deployment.yaml  # Bundled PostgreSQL
│   └── postgresql-service.yaml     # PostgreSQL Service
└── README.md               # This file
```

## Quick Start

### 1. Build the Application

```bash
mvn clean package -DskipTests
```

### 2. Build the Docker Image

```bash
docker build -t model-lite-repository:0.1.0-SNAPSHOT .
```

### 3. Load Image into Cluster

Choose one method based on your cluster setup:

**For Minikube:**
```bash
eval $(minikube docker-env)
docker build -t model-lite-repository:0.1.0-SNAPSHOT .
```

**For Kind:**
```bash
kind load docker-image model-lite-repository:0.1.0-SNAPSHOT
```

**For K3s:**
```bash
docker save model-lite-repository:0.1.0-SNAPSHOT | k3s ctr images import -
```

**For a remote registry:**
```bash
docker tag model-lite-repository:0.1.0-SNAPSHOT <registry>/model-lite-repository:0.1.0-SNAPSHOT
docker push <registry>/model-lite-repository:0.1.0-SNAPSHOT
```

### 4. Create Namespace

```bash
kubectl create namespace modellite-dev
```

### 5. Deploy with Helm

```bash
helm install model-lite-repository ./helm \
  --namespace modellite-dev \
  --set image.repository=model-lite-repository \
  --set image.tag=0.1.0-SNAPSHOT
```

### 6. Verify Deployment

```bash
# Check pod status
kubectl get pods -n modellite-dev

# Check service
kubectl get svc -n modellite-dev

# View logs
kubectl logs -n modellite-dev -l app.kubernetes.io/name=model-lite-repository -f

# Port-forward for local access
kubectl port-forward -n modellite-dev svc/model-lite-repository 8080:8080
```

Access the application at `http://localhost:8080`.

## Configuration

### Key Values

| Parameter | Default | Description |
|-----------|---------|-------------|
| `replicaCount` | `1` | Number of pod replicas |
| `image.repository` | `model-lite-repository` | Container image name |
| `image.tag` | `0.1.0-SNAPSHOT` | Image tag |
| `service.type` | `ClusterIP` | Service type (ClusterIP / NodePort / LoadBalancer) |
| `service.port` | `8080` | Service port |
| `app.profiles` | `dev` | Active Spring profiles |
| `database.host` | _(auto)_ | PostgreSQL host (auto-set when postgresql.enabled=true) |
| `database.name` | `modellite` | Database name |
| `database.username` | `modellite` | Database username |
| `database.password` | `changeme` | Database password |
| `database.existingSecret` | `""` | Use existing Secret for DB credentials |
| `postgresql.enabled` | `true` | Deploy bundled PostgreSQL StatefulSet |
| `postgresql.image.repository` | `postgres` | PostgreSQL image (supports arm64) |
| `postgresql.image.tag` | `15` | PostgreSQL version |
| `postgresql.persistence.hostPath` | `/tmp/postgresql-data` | Node hostPath for data (debug only) |
| `postgresql.resources` | _(see values)_ | CPU/memory limits for PostgreSQL |

### Custom Values File

Create `values-override.yaml`:

```yaml
image:
  repository: myregistry/model-lite-repository
  tag: 0.1.0-SNAPSHOT

database:
  host: my-postgres.default.svc.cluster.local
  name: modellite_dev
  username: myuser
  password: mypassword

service:
  type: NodePort
  nodePort: 30080

resources:
  limits:
    cpu: 2000m
    memory: 2Gi
  requests:
    cpu: 500m
    memory: 1Gi
```

Deploy with overrides:

```bash
helm install model-lite-repository ./helm \
  --namespace modellite-dev \
  -f values-override.yaml
```

### Using Existing Database Secret

If your cluster already manages DB credentials via a Secret:

```bash
helm install model-lite-repository ./helm \
  --namespace modellite-dev \
  --set database.existingSecret=my-db-secret
```

The referenced Secret must contain keys `username` and `password`.

### Using External PostgreSQL

If you already have a PostgreSQL instance running in the cluster:

```bash
helm install model-lite-repository ./helm \
  --namespace modellite-dev \
  --set postgresql.enabled=false \
  --set database.host=my-postgres.default.svc.cluster.local
```

### Bundled PostgreSQL

By default, a PostgreSQL 15 Deployment is deployed alongside the application with hostPath storage:

```bash
# Default deployment (includes PostgreSQL)
helm install model-lite-repository ./helm --namespace modellite-dev

# Customize data path
helm install model-lite-repository ./helm \
  --namespace modellite-dev \
  --set postgresql.persistence.hostPath=/data/postgres
```

The application automatically connects to the bundled PostgreSQL via the internal service name. No need to set `database.host` manually.

## Upgrade & Rollback

```bash
# Upgrade
helm upgrade model-lite-repository ./helm \
  --namespace modellite-dev \
  --set image.tag=0.1.1-SNAPSHOT

# View release history
helm history model-lite-repository -n modellite-dev

# Rollback to previous revision
helm rollback model-lite-repository -n modellite-dev

# Rollback to specific revision
helm rollback model-lite-repository 1 -n modellite-dev
```

## Uninstall

```bash
helm uninstall model-lite-repository -n modellite-dev
kubectl delete namespace modellite-dev
```

## Health Endpoints

The application exposes Spring Boot Actuator health endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health/liveness` | Liveness probe — pod restart if failing |
| `/actuator/health/readiness` | Readiness probe — traffic routing if failing |
| `/actuator/health` | Overall health status |

## Troubleshooting

**Pod in ImagePullBackOff:** Image not found in cluster. Re-run step 3 (Load Image into Cluster).

**Pod CrashLoopBackOff — DB connection failed:** Verify `database.host` points to a running PostgreSQL instance. Check with `kubectl get svc -n <namespace>`.

**Pod CrashLoopBackOff — port conflict:** Ensure `app.serverPort` matches the container port and no other process binds to 8080.

**Debug with ephemeral container:**
```bash
kubectl debug -n modellite-dev -it <pod-name> --image=busybox --target=model-lite-repository
```
