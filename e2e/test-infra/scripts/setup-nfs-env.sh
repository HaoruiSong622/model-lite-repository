#!/bin/bash
set -euo pipefail

KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-modellite}"
KIND_NODE="${KIND_CLUSTER_NAME}-control-plane"

if ! grep -qw nfs /proc/filesystems 2>/dev/null; then
  echo "[setup] loading nfs kernel modules (WSL2 ships them as module, not loaded by default)..."
  modprobe nfs || { echo "[setup] ERROR: modprobe nfs failed; WSL2 kernel may lack nfs support"; exit 1; }
  modprobe nfsd || { echo "[setup] ERROR: modprobe nfsd failed"; exit 1; }
fi
grep -qw nfs /proc/filesystems || { echo "[setup] ERROR: nfs still not in /proc/filesystems after modprobe"; exit 1; }
echo "[setup] nfs kernel support OK"

if ! docker exec "$KIND_NODE" which mount.nfs >/dev/null 2>&1; then
  echo "[setup] ERROR: mount.nfs not found in kind node $KIND_NODE (nfs-common missing)"
  exit 1
fi
echo "[setup] kind node nfs-common OK (mount.nfs present)"
