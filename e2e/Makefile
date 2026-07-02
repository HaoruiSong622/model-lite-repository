E2E_NS ?= modellite-dev
E2E_PORT ?= 8080
KIND_NAME ?= modellite

e2e-setup: e2e-nfs e2e-app e2e-portforward

e2e-nfs:
	bash e2e/test-infra/scripts/setup-nfs-env.sh
	bash e2e/test-infra/scripts/deploy-nfs-server.sh

e2e-copier:
	bash e2e/test-infra/scripts/build-file-copier.sh

e2e-app: e2e-copier
	helm upgrade --install model-lite-repository ./helm -n $(E2E_NS) --create-namespace \
	  --set image.repository=model-lite-repository --set image.tag=0.1.0-SNAPSHOT \
	  --set weightImport.job.image=modellite/file-copier:latest
	kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=model-lite-repository \
	  -n $(E2E_NS) --timeout=300s

e2e-portforward:
	-@pkill -f "kubectl port-forward.*$(E2E_PORT):8080" 2>/dev/null || true
	kubectl port-forward -n $(E2E_NS) svc/model-lite-repository $(E2E_PORT):8080 &
	@for i in 1 2 3 4 5 6 7 8 9 10; do \
	  curl -s http://localhost:$(E2E_PORT)/actuator/health >/dev/null 2>&1 && break; \
	  sleep 2; \
	done

e2e-test:
	cd e2e && E2E_BASE_URL=http://localhost:$(E2E_PORT) pytest --alluredir=allure-results -v

e2e-report:
	allure serve e2e/allure-results

e2e-down:
	-helm uninstall model-lite-repository -n $(E2E_NS) || true
	bash e2e/test-infra/scripts/cleanup-nfs-server.sh
	-@pkill -f "kubectl port-forward.*$(E2E_PORT):8080" 2>/dev/null || true
