#!/bin/bash
set -e

dd if=/dev/zero of=/export.img bs=1M count=20 2>/dev/null
mkfs.ext4 -F /export.img 2>/dev/null
mkdir -p /exports
mount -o loop /export.img /exports
echo "ext4 loop mounted on /exports"

mkdir -p /exports/models
head -c 1048576 /dev/urandom > /exports/models/fake-1.safetensors
head -c 1048576 /dev/urandom > /exports/models/fake-2.bin
head -c 1024 /dev/urandom > /exports/models/readme.txt
echo "seed files:"; ls -la /exports/models

echo "/exports *(rw,sync,no_subtree_check,insecure,no_root_squash,fsid=0)" > /etc/exports
echo "/exports/models *(rw,sync,no_subtree_check,insecure,no_root_squash)" >> /etc/exports

rpcbind -d &
sleep 2
rpc.nfsd 8
echo "nfsd started"
rpc.mountd
echo "mountd started"
exportfs -rav
echo "exports:"
exportfs -v
exec sleep infinity
