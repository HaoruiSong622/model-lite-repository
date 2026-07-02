import os
import subprocess
import pathlib

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


@pytest.fixture(scope="session", autouse=True)
def cleanup_session(pg):
    yield
    try:
        pg.cleanup_by_name_prefix("e2e-")
    except Exception as e:
        print(f"[e2e] cleanup warning: {e}")


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()
    if report.when == "call" and report.failed:
        artifact_dir = pathlib.Path("artifacts") / item.name
        artifact_dir.mkdir(parents=True, exist_ok=True)
        ns = os.environ.get("E2E_NAMESPACE", "modellite-dev")
        try:
            out = subprocess.run(
                ["kubectl", "logs", "-n", ns,
                 "-l", "app.kubernetes.io/name=model-lite-repository", "--tail=100"],
                capture_output=True, text=True, timeout=30,
            )
            (artifact_dir / "app-logs.txt").write_text(out.stdout)
        except Exception:
            pass
