package com.huawei.modellite.repository.weighttask.domain.service;

import com.huawei.modellite.repository.infrastructure.config.WeightImportConfig;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TaskEventCallbackTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private UploadTaskRepository uploadTaskRepository;

    @Mock
    private K8sJobService k8sJobService;

    @Mock
    private WeightImportConfig weightImportConfig;

    @Test
    @DisplayName("UploadApplicationService should implement TaskEventCallback")
    void uploadApplicationServiceImplementsTaskEventCallback() {
        UploadApplicationService service = new UploadApplicationService(
                modelRepository, uploadTaskRepository, k8sJobService, weightImportConfig);

        assertInstanceOf(TaskEventCallback.class, service,
                "UploadApplicationService should be an instance of TaskEventCallback");
    }

    @Test
    @DisplayName("TaskEventCallback interface defines all four required methods")
    void taskEventCallbackDefinesRequiredMethods() throws NoSuchMethodException {
        assertNotNull(TaskEventCallback.class.getMethod("onJobRunning", String.class),
                "TaskEventCallback should define onJobRunning(String)");

        assertNotNull(TaskEventCallback.class.getMethod("onJobCompleted", String.class),
                "TaskEventCallback should define onJobCompleted(String)");

        assertNotNull(TaskEventCallback.class.getMethod("onJobFailed", String.class, String.class),
                "TaskEventCallback should define onJobFailed(String, String)");

        assertNotNull(TaskEventCallback.class.getMethod("updateProgress", String.class, Integer.class),
                "TaskEventCallback should define updateProgress(String, Integer)");
    }

    @Test
    @DisplayName("UploadApplicationService callback methods are callable via TaskEventCallback interface")
    void callbackMethodsAreCallableViaInterface() {
        UploadApplicationService service = new UploadApplicationService(
                modelRepository, uploadTaskRepository, k8sJobService, weightImportConfig);

        TaskEventCallback callback = service;

        assertSame(service, callback,
                "TaskEventCallback reference should point to the same service instance");
    }
}