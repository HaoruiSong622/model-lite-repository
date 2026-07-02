import pytest

from factories.builders import ModelBuilder, UploadTaskBuilder
from helpers.assertions import assert_success
from helpers.waiters import wait_for_task_status

BUILTIN_CATEGORY = "10000000-0000-0000-0000-000000000001"
BUILTIN_TYPE = "20000000-0000-0000-0000-000000000001"


@pytest.mark.e2e
def test_nfs_upload_full_flow(client, k8s):
    model_id = None
    version_id = None
    try:
        model_resp = client.create_model(
            ModelBuilder().with_category(BUILTIN_CATEGORY, BUILTIN_TYPE).build())
        model_body = assert_success(model_resp)
        model_id = model_body["data"]["id"]

        task_resp = client.create_upload_task(
            model_id, UploadTaskBuilder.nfs("nfs-server-svc", "/models"))
        task_body = assert_success(task_resp)
        task_id = task_body["data"]["taskId"]
        version_id = task_body["data"]["versionId"]

        wait_for_task_status(client, model_id, task_id, "Completed", timeout=120)

        files = k8s.list_pvc_files(model_id, version_id)
        assert any(f.endswith(".safetensors") or f.endswith(".bin") for f in files), \
            f"expected weight files in PVC, got: {files}"
    finally:
        pod = k8s.get_app_pod_name()
        k8s.pod_logs(pod, tail=20)
        k8s.list_pvc_files(model_id or "probe-model", version_id or "probe-version")
