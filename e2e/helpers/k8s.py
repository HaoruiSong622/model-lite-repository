import json
import subprocess
import time

import allure


class KubectlHelper:
    def __init__(self, namespace="modellite-dev", app_label="app.kubernetes.io/name=model-lite-repository",
                 node_name="modellite-control-plane"):
        self.namespace = namespace
        self.app_label = app_label
        self.node_name = node_name

    def _run(self, args, check=True):
        cmd = ["kubectl"] + args
        result = subprocess.run(cmd, capture_output=True, text=True, check=check)
        return result.stdout

    def _run_on_node(self, shell_cmd):
        cmd = ["docker", "exec", self.node_name, "bash", "-c", shell_cmd]
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
        return result.stdout

    def list_nfs_source_files(self, nfs_server="nfs-server-svc", nfs_path="/models"):
        pod_name = f"nfs-probe-{int(time.time())}"
        overrides = json.dumps({
            "spec": {
                "containers": [{
                    "name": "probe",
                    "image": "nginx-patched:latest",
                    "imagePullPolicy": "IfNotPresent",
                    "command": ["sh", "-c", "ls -la /src"],
                    "volumeMounts": [{"name": "nfs", "mountPath": "/src"}]
                }],
                "volumes": [{
                    "name": "nfs",
                    "nfs": {"server": nfs_server, "path": nfs_path}
                }],
                "restartPolicy": "Never"
            }
        })
        self._run(["run", pod_name, "-n", self.namespace,
                   "--image=nginx-patched:latest", "--restart=Never",
                   f"--overrides={overrides}"], check=False)
        time.sleep(10)
        out = self._run(["logs", pod_name, "-n", self.namespace], check=False)
        self._run(["delete", "pod", pod_name, "-n", self.namespace, "--ignore-not-found"], check=False)
        self._attach(f"$ kubectl run {pod_name} (NFS {nfs_server}:{nfs_path} -> /src)\n{out}",
                     f"NFS 源文件(上传前): {nfs_server}:{nfs_path}", allure.attachment_type.TEXT)
        return out
        self._attach(f"$ mount -t nfs4 {nfs_server}:{nfs_path} && ls -la\n{out}",
                     f"NFS 源文件(上传前): {nfs_server}:{nfs_path}", allure.attachment_type.TEXT)
        return out

    def get_job(self, name):
        try:
            out = self._run(["get", "job", name, "-n", self.namespace, "-o", "json"], check=False)
            if not out.strip() or "NotFound" in out:
                return None
            return json.loads(out)
        except Exception:
            return None

    def get_job_pod_name_by_task_id(self, task_id):
        out = self._run(["get", "pod", "-n", self.namespace,
                         "-l", f"modellite/upload-task-id={task_id}",
                         "-o", "jsonpath={.items[0].metadata.name}"], check=False)
        name = out.strip()
        return name or None

    def get_job_pod_logs(self, task_id, tail=50):
        pod = self.get_job_pod_name_by_task_id(task_id)
        if not pod:
            self._attach("(file-copier pod 已被清除，无法获取日志)",
                         f"file-copier Job 输出: {task_id}", allure.attachment_type.TEXT)
            return ""
        out = self._run(["logs", pod, "-n", self.namespace, f"--tail={tail}"], check=False)
        self._attach(out, f"file-copier Job 输出: {pod}", allure.attachment_type.TEXT)
        return out

    def get_app_pod_name(self):
        out = self._run(["get", "pod", "-n", self.namespace, "-l", self.app_label,
                         "-o", "jsonpath={.items[0].metadata.name}"])
        return out.strip()

    def list_pvc_files(self, model_id, version_id):
        pod = self.get_app_pod_name()
        path = f"/data/weights/{model_id}/{version_id}"
        try:
            out = self._run(["exec", pod, "-n", self.namespace, "--", "ls", "-la", path], check=False)
            files = [f for f in out.strip().split("\n") if f]
            self._attach(f"$ ls -la {path}\n{out}",
                         f"PVC 文件(上传后): {model_id}/{version_id}", allure.attachment_type.TEXT)
            return files
        except Exception:
            return []

    def pod_logs(self, pod_name, tail=50):
        out = self._run(["logs", pod_name, "-n", self.namespace, f"--tail={tail}"])
        self._attach(out, f"pod logs: {pod_name}", allure.attachment_type.TEXT)
        return out

    def wait_for_job_deleted(self, task_id, timeout=90, poll_interval=5):
        job_name = f"upload-{task_id}"
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.get_job(job_name) is None:
                self._attach(f"Job {job_name} 已被应用自动清除",
                             f"Job 清除验证: {task_id}", allure.attachment_type.TEXT)
                return True
            time.sleep(poll_interval)
        self._attach(f"Job {job_name} 仍存在(超时未清除)",
                     f"Job 清除验证(超时): {task_id}", allure.attachment_type.TEXT)
        return False

    @staticmethod
    def _attach(body, name, attachment_type):
        try:
            allure.attach(body, name=name, attachment_type=attachment_type)
        except Exception:
            pass
