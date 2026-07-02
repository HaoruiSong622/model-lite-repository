import logging

import requests

logger = logging.getLogger(__name__)


class ModelLiteClient:
    def __init__(self, base_url, create_user="e2e"):
        self.base_url = base_url.rstrip("/")
        self.create_user = create_user
        self.s = requests.Session()
        self.s.headers.update({"Content-Type": "application/json"})

    def _req(self, method, path, *, params=None, json=None, **kwargs):
        url = f"{self.base_url}{path}"
        resp = self.s.request(method, url, params=params, json=json, timeout=30, **kwargs)
        if resp.status_code >= 500:
            logger.error("5xx %s %s: %s", method, url, resp.text[:500])
        return resp

    def health(self):
        return self._req("GET", "/actuator/health")

    def create_model(self, payload):
        return self._req("POST", "/v2/ui/models", json=payload)

    def get_model(self, model_id, resource_group=None):
        params = {"resourceGroup": resource_group} if resource_group else None
        return self._req("GET", f"/v2/ui/models/{model_id}", params=params)

    def modify_model(self, model_id, payload, resource_group=None):
        params = {"resourceGroup": resource_group} if resource_group else None
        return self._req("PATCH", f"/v2/ui/models/{model_id}", json=payload, params=params)

    def list_models(self, **params):
        filtered = {k: v for k, v in params.items() if v is not None}
        return self._req("GET", "/v2/ui/models", params=filtered)

    def create_version(self, model_id, payload, resource_group=None):
        params = {"resourceGroup": resource_group} if resource_group else None
        return self._req("POST", f"/v2/ui/models/{model_id}/versions", json=payload, params=params)

    def get_version(self, model_id, version_id, resource_group=None):
        params = {"resourceGroup": resource_group} if resource_group else None
        return self._req("GET", f"/v2/ui/models/{model_id}/versions/{version_id}", params=params)

    def register_version(self, model_id, version_id, payload, resource_group=None):
        params = {"resourceGroup": resource_group} if resource_group else None
        return self._req("POST", f"/v2/ui/models/{model_id}/versions/{version_id}/register", json=payload, params=params)

    def create_upload_task(self, model_id, payload, create_user=None):
        params = {"createUser": create_user or self.create_user}
        return self._req("POST", f"/v2/ui/models/{model_id}/upload-tasks", json=payload, params=params)

    def get_upload_task(self, model_id, task_id):
        return self._req("GET", f"/v2/ui/models/{model_id}/upload-tasks/{task_id}")

    def list_upload_tasks(self, model_id, status=None):
        params = {"status": status} if status else None
        return self._req("GET", f"/v2/ui/models/{model_id}/upload-tasks", params=params)

    def pause_upload_task(self, model_id, task_id):
        return self._req("POST", f"/v2/ui/models/{model_id}/upload-tasks/{task_id}/pause")

    def resume_upload_task(self, model_id, task_id):
        return self._req("POST", f"/v2/ui/models/{model_id}/upload-tasks/{task_id}/resume")

    def cancel_upload_task(self, model_id, task_id):
        return self._req("POST", f"/v2/ui/models/{model_id}/upload-tasks/{task_id}/cancel")

    def delete_upload_task(self, model_id, task_id):
        return self._req("DELETE", f"/v2/ui/models/{model_id}/upload-tasks/{task_id}")

    def list_categories(self):
        return self._req("GET", "/v2/ui/categories")

    def get_category(self, category_id):
        return self._req("GET", f"/v2/ui/categories/{category_id}")

    def create_category(self, payload):
        return self._req("POST", "/v2/ui/categories", json=payload)

    def delete_category(self, category_id):
        return self._req("DELETE", f"/v2/ui/categories/{category_id}")

    def add_type_to_category(self, category_id, name, description=None):
        return self._req("POST", f"/v2/ui/categories/{category_id}/types",
                         json={"name": name, "description": description})

    def remove_type_from_category(self, category_id, type_id):
        return self._req("DELETE", f"/v2/ui/categories/{category_id}/types/{type_id}")

    def list_tags(self):
        return self._req("GET", "/v2/ui/tags")

    def create_tag(self, payload):
        return self._req("POST", "/v2/ui/tags", json=payload)

    def delete_tag(self, tag_id):
        return self._req("DELETE", f"/v2/ui/tags/{tag_id}")

    def add_tags_to_model(self, model_id, tag_ids):
        return self._req("POST", f"/v2/ui/models/{model_id}/tags", json=tag_ids)

    def remove_tag_from_model(self, model_id, tag_id):
        return self._req("DELETE", f"/v2/ui/models/{model_id}/tags/{tag_id}")

    def archive_version(self, model_id, version_id, payload):
        return self._req("POST", f"/v2/models/{model_id}/versions/{version_id}/archive", json=payload)
