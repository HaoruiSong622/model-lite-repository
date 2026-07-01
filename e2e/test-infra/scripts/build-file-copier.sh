#!/bin/bash
set -euo pipefail

KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-modellite}"
IMAGE_NAME="${IMAGE_NAME:-modellite/file-copier}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"

echo "[build] building ${IMAGE_NAME}:${IMAGE_TAG}..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" "${INFRA_DIR}/file-copier"

echo "[build] loading into kind cluster ${KIND_CLUSTER_NAME}..."
kind load docker-image "${IMAGE_NAME}:${IMAGE_TAG}" --name "$KIND_CLUSTER_NAME"

echo "[build] done. ${IMAGE_NAME}:${IMAGE_TAG} available in cluster."
echo "  note: app config weight-import.job.image should point to this image."
echo "  TaskReconciler.rebuildJob hardcodes 'modellite/file-copier:latest' — keep IMAGE_NAME/IMAGE_TAG aligned."
