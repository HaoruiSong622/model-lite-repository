E2E_NS ?= modellite-dev
E2E_PORT ?= 8080
KIND_NAME ?= modellite
CONDA_ENV ?= model-lite
CONDA_PY ?= $(HOME)/miniconda3/envs/$(CONDA_ENV)/bin/python

e2e-setup: e2e-env e2e-nfs e2e-copier e2e-app e2e-portforward

e2e-env:
	bash e2e/test-infra/scripts/setup-nfs-env.sh

e2e-nfs:
	bash e2e/test-infra/scripts/deploy-nfs-server.sh

e2e-copier:
	bash e2e/test-infra/scripts/build-file-copier.sh

e2e-app:
	helm upgrade --install model-lite-repository ./helm -n $(E2E_NS) --create-namespace \
	  --set image.repository=model-lite-repository --set image.tag=0.1.0-SNAPSHOT \
	  --set weightImport.job.image=modellite/file-copier:latest \
	  --set postgresql.image.repository=pg15-kind --set postgresql.image.tag=latest \
	  --set initContainers.dbMigrate.image.repository=pg15-kind --set initContainers.dbMigrate.image.tag=latest
	kubectl wait --for=condition=ready pod \
	  -l app.kubernetes.io/name=model-lite-repository,app.kubernetes.io/component!=postgresql \
	  -n $(E2E_NS) --timeout=300s

e2e-portforward:
	-@tmux kill-session -t pf 2>/dev/null || true
	@APPPOD=$$(kubectl get pod -n $(E2E_NS) \
	  -l app.kubernetes.io/name=model-lite-repository,app.kubernetes.io/component!=postgresql \
	  -o jsonpath='{.items[0].metadata.name}'); \
	tmux new-session -d -s pf "kubectl port-forward -n $(E2E_NS) pod/$$APPPOD $(E2E_PORT):8080"; \
	for i in 1 2 3 4 5 6 7 8; do \
	  curl -s http://localhost:$(E2E_PORT)/actuator/health >/dev/null 2>&1 \
	    && echo "app ready at :$(E2E_PORT)" && break; sleep 2; \
	done

e2e-test:
	cd e2e && E2E_BASE_URL=http://localhost:$(E2E_PORT) $(CONDA_PY) -m pytest --alluredir=allure-results -v

e2e-report:
	allure serve e2e/allure-results

e2e-down:
	-helm uninstall model-lite-repository -n $(E2E_NS) || true
	bash e2e/test-infra/scripts/cleanup-nfs-server.sh
	-@tmux kill-session -t pf 2>/dev/null || true
