import pytest

from factories.builders import ModelBuilder, UploadTaskBuilder
from helpers.assertions import assert_success
from helpers.waiters import wait_for_task_status

BUILTIN_CATEGORY = "10000000-0000-0000-0000-000000000001"
BUILTIN_TYPE = "20000000-0000-0000-0000-000000000001"
NFS_SERVER = "nfs-server-svc"
NFS_PATH = "/models"


@pytest.mark.e2e
def test_nfs_upload_full_flow(client, k8s):
    k8s.list_nfs_source_files(NFS_SERVER, NFS_PATH)

    model_resp = client.create_model(
        ModelBuilder().with_category(BUILTIN_CATEGORY, BUILTIN_TYPE).build())
    model_body = assert_success(model_resp)
    model_id = model_body["data"]["id"]

    task_resp = client.create_upload_task(
        model_id, UploadTaskBuilder.nfs(NFS_SERVER, NFS_PATH))
    task_body = assert_success(task_resp)
    task_id = task_body["data"]["taskId"]
    version_id = task_body["data"]["versionId"]

    wait_for_task_status(client, model_id, task_id, "Completed", timeout=120)

    k8s.get_job_pod_logs(task_id)

    files = k8s.list_pvc_files(model_id, version_id)
    assert any(f.endswith(".safetensors") or f.endswith(".bin") for f in files), \
        f"expected weight files in PVC, got: {files}"

    assert k8s.wait_for_job_deleted(task_id, timeout=90), \
        f"Job upload-{task_id} was not auto-cleaned after task Completed"
