import json
import os
import subprocess

import allure
import pytest

from helpers.client import ModelLiteClient
from helpers.db import PgHelper
from helpers.k8s import KubectlHelper


@pytest.fixture(scope="session")
def base_url():
    return os.environ.get("E2E_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def e2e_namespace():
    return os.environ.get("E2E_NAMESPACE", "modellite-dev")


@pytest.fixture(scope="session")
def client(base_url):
    return ModelLiteClient(base_url, create_user="e2e")


@pytest.fixture(scope="session")
def k8s(e2e_namespace):
    return KubectlHelper(namespace=e2e_namespace)


@pytest.fixture(scope="session")
def pg():
    return PgHelper(
        host=os.environ.get("PG_HOST", "localhost"),
        port=int(os.environ.get("PG_PORT", "5432")),
        dbname=os.environ.get("PG_DB", "modellite"),
        user=os.environ.get("PG_USER", "modellite"),
        password=os.environ.get("PG_PASSWORD", "changeme"),
    )


@pytest.fixture(autouse=True)
def _clear_history(client):
    client.clear_history()
    yield


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    setattr(item, f"rep_{rep.when}", rep)
    if rep.when != "call":
        return
    client = item.funcargs.get("client") if hasattr(item, "funcargs") else None
    if client and getattr(client, "history", None):
        lines = []
        for i, h in enumerate(client.history, 1):
            lines.append(f"[{i}] {h['method']} {h['url']}")
            if h.get("params"):
                lines.append(f"    Params: {h['params']}")
            if h["request_body"] is not None:
                lines.append(f"    Request body: {json.dumps(h['request_body'], ensure_ascii=False)}")
            lines.append(f"    Response [{h['status_code']}]: {h['response_body']}")
            lines.append("")
        allure.attach("\n".join(lines), name="API 请求历史",
                      attachment_type=allure.attachment_type.TEXT)
    if rep.failed:
        ns = os.environ.get("E2E_NAMESPACE", "modellite-dev")
        try:
            out = subprocess.run(
                ["kubectl", "logs", "-n", ns,
                 "-l", "app.kubernetes.io/name=model-lite-repository", "--tail=100"],
                capture_output=True, text=True, timeout=30,
            )
            allure.attach(out.stdout, name="应用 pod 日志(失败时)",
                          attachment_type=allure.attachment_type.TEXT)
        except Exception:
            pass


@pytest.fixture(scope="session", autouse=True)
def cleanup_session(pg):
    yield
    try:
        pg.cleanup_by_name_prefix("e2e-")
    except Exception as e:
        print(f"[e2e] cleanup warning: {e}")
