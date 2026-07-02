import uuid


class CategoryBuilder:
    def __init__(self):
        self._data = {"name": f"e2e-cat-{uuid.uuid4()}", "description": "e2e test category"}

    def with_name(self, name):
        self._data["name"] = name
        return self

    def build(self):
        return dict(self._data)


class ModelBuilder:
    def __init__(self):
        self._data = {"name": f"e2e-model-{uuid.uuid4()}", "description": "e2e test model",
                      "resourceGroup": "e2e-rg", "author": "e2e"}

    def with_name(self, name):
        self._data["name"] = name
        return self

    def with_category(self, category_id, type_id=None):
        self._data["categoryId"] = str(category_id)
        if type_id:
            self._data["typeId"] = str(type_id)
        return self

    def with_resource_group(self, rg):
        self._data["resourceGroup"] = rg
        return self

    def build(self):
        return dict(self._data)


class VersionCreateRequestBuilder:
    def __init__(self):
        self._data = {"registered": True, "sourceType": "PVC"}

    def registered_nfs(self, nfs_server, nfs_path, weight_type="safetensors"):
        self._data.update(registered=True, sourceType="NFS",
                          nfsServer=nfs_server, nfsPath=nfs_path, weightType=weight_type)
        return self

    def registered_pvc(self, pvc_name, internal_path, weight_type="safetensors"):
        self._data.update(registered=True, sourceType="PVC",
                          pvcName=pvc_name, internalPath=internal_path, weightType=weight_type)
        return self

    def build(self):
        return dict(self._data)


class UploadTaskBuilder:
    @staticmethod
    def nfs(nfs_server, nfs_path, weight_type="safetensors"):
        return {"sourceType": "NFS", "nfsServer": nfs_server,
                "nfsPath": nfs_path, "weightType": weight_type}

    @staticmethod
    def pvc(pvc_name, internal_path, weight_type="safetensors"):
        return {"sourceType": "PVC", "sourcePvcName": pvc_name,
                "sourceInternalPath": internal_path, "weightType": weight_type}

    @staticmethod
    def cifs(server, share, username, password, weight_type="safetensors"):
        return {"sourceType": "CIFS", "cifsServer": server, "cifsShare": share,
                "cifsUsername": username, "cifsPassword": password, "weightType": weight_type}
