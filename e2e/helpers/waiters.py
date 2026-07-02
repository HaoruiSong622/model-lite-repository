import time

import pytest

TERMINAL_STATUSES = {"Completed", "Failed", "Cancelled"}


def wait_for_task_status(client, model_id, task_id, target_status,
                         timeout=300, poll_interval=2):
    deadline = time.time() + timeout
    last_status = None
    while time.time() < deadline:
        resp = client.get_upload_task(model_id, task_id)
        assert resp.status_code == 200, f"get_upload_task failed: {resp.status_code} {resp.text}"
        last_status = resp.json()["data"]["status"]
        if last_status == target_status:
            return last_status
        if last_status in TERMINAL_STATUSES and last_status != target_status:
            pytest.fail(f"task reached terminal status {last_status} before {target_status}")
        time.sleep(poll_interval)
    pytest.fail(f"timeout waiting task {task_id} -> {target_status}, last={last_status}")
