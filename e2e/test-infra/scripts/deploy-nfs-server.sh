#!/bin/bash
set -euo pipefail

KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-modellite}"
KIND_NODE="${KIND_CLUSTER_NAME}-control-plane"
NAMESPACE="${NAMESPACE:-modellite-dev}"
IMAGE_NAME="${IMAGE_NAME:-e2e-nfs-server}"
IMAGE_TAG="${IMAGE_TAG:-v0.1.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"

echo "[deploy] building image ${IMAGE_NAME}:${IMAGE_TAG}..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" "${INFRA_DIR}/nfs-server"

echo "[deploy] loading image into kind cluster ${KIND_CLUSTER_NAME}..."
kind load docker-image "${IMAGE_NAME}:${IMAGE_TAG}" --name "$KIND_CLUSTER_NAME"

echo "[deploy] applying nfs-server.yaml to namespace ${NAMESPACE}..."
kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || kubectl create namespace "$NAMESPACE"
kubectl apply -n "$NAMESPACE" -f "${INFRA_DIR}/nfs-server.yaml"

echo "[deploy] waiting for e2e-nfs-server pod ready (up to 120s)..."
kubectl rollout status deployment/e2e-nfs-server -n "$NAMESPACE" --timeout=120s

CLUSTER_IP=$(kubectl get svc nfs-server-svc -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}')
echo "[deploy] nfs-server-svc ClusterIP: ${CLUSTER_IP}"

# kind node resolv.conf points to Docker DNS (172.18.0.1), not CoreDNS, so
# kubelet cannot resolve Service names like nfs-server-svc when mounting NFS
# volume. Fix: map the Service name to its ClusterIP in node /etc/hosts.
docker exec "$KIND_NODE" bash -c "grep -v 'nfs-server-svc' /etc/hosts > /tmp/hosts.tmp && cat /tmp/hosts.tmp > /etc/hosts && echo '${CLUSTER_IP} nfs-server-svc' >> /etc/hosts"
echo "[deploy] node /etc/hosts patched (nfs-server-svc -> ${CLUSTER_IP})"

echo "[deploy] verifying nfs server registration via rpcinfo..."
POD_IP=$(kubectl get pod -n "$NAMESPACE" -l app=e2e-nfs-server -o jsonpath='{.items[0].status.podIP}')
for i in 1 2 3 4 5; do
  if docker exec "$KIND_NODE" rpcinfo -p "$POD_IP" 2>/dev/null | grep -q "nfs"; then
    echo "[deploy] OK: nfs server registered and reachable (attempt $i)"
    break
  fi
  [ "$i" -eq 5 ] && { echo "[deploy] WARN: rpcinfo failed after 5 attempts; pod logs:"; kubectl logs -n "$NAMESPACE" -l app=e2e-nfs-server --tail=20; exit 1; }
  echo "[deploy] rpcinfo attempt $i failed, retrying in 3s..."; sleep 3
done

echo ""
echo "[deploy] DONE. NFS test source ready:"
echo "  server = nfs-server-svc"
echo "  path   = /models"
echo "  files  = fake-1.safetensors, fake-2.bin (whitelist), readme.txt (non-whitelist)"
