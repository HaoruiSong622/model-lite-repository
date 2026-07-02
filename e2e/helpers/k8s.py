import json
import subprocess


class KubectlHelper:
    def __init__(self, namespace="modellite-dev", app_label="app.kubernetes.io/name=model-lite-repository"):
        self.namespace = namespace
        self.app_label = app_label

    def _run(self, args, check=True):
        cmd = ["kubectl"] + args
        result = subprocess.run(cmd, capture_output=True, text=True, check=check)
        return result.stdout

    def get_job(self, name):
        try:
            out = self._run(["get", "job", name, "-n", self.namespace, "-o", "json"], check=False)
            if not out.strip() or "NotFound" in out:
                return None
            return json.loads(out)
        except Exception:
            return None

    def get_app_pod_name(self):
        out = self._run(["get", "pod", "-n", self.namespace, "-l", self.app_label,
                         "-o", "jsonpath={.items[0].metadata.name}"])
        return out.strip()

    def list_pvc_files(self, model_id, version_id):
        pod = self.get_app_pod_name()
        path = f"/data/weights/{model_id}/{version_id}"
        try:
            out = self._run(["exec", pod, "-n", self.namespace, "--", "ls", "-1", path], check=False)
            return [f for f in out.strip().split("\n") if f]
        except Exception:
            return []

    def pod_logs(self, pod_name, tail=50):
        return self._run(["logs", pod_name, "-n", self.namespace, f"--tail={tail}"])
