#!/bin/bash
set -euo pipefail

KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-modellite}"
KIND_NODE="${KIND_CLUSTER_NAME}-control-plane"
NAMESPACE="${NAMESPACE:-modellite-dev}"

echo "[cleanup] deleting nfs-server deployment and service in ${NAMESPACE}..."
kubectl delete deployment e2e-nfs-server -n "$NAMESPACE" --ignore-not-found
kubectl delete service nfs-server-svc -n "$NAMESPACE" --ignore-not-found

docker exec "$KIND_NODE" bash -c "grep -v 'nfs-server-svc' /etc/hosts > /tmp/hosts.tmp && cat /tmp/hosts.tmp > /etc/hosts" 2>/dev/null || true
echo "[cleanup] node /etc/hosts nfs-server-svc entry removed"
echo "[cleanup] done"
