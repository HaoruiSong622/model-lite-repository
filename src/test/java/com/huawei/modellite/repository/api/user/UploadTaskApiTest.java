package com.huawei.modellite.repository.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskCreateRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskResponse;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadTaskApi.class)
class UploadTaskApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadApplicationService uploadApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID MODEL_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TASK_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID VERSION_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    @Test
    void createUploadTask_shouldReturnCreatedTask() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("PVC");
        request.setSourcePvcName("test-pvc");
        request.setSourceInternalPath("/models");
        request.setWeightType("FP32");

        UploadTaskResponse response = createUploadTaskResponse(TASK_ID, "Running");
        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("admin")))
                .thenReturn(response);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .param("createUser", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("Running"));
    }

    @Test
    void createUploadTask_shouldReturnCreatedTask_withNfs() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("NFS");
        request.setNfsServer("192.168.1.100");
        request.setNfsPath("/exports/models");
        request.setWeightType("FP32");

        UploadTaskResponse response = createUploadTaskResponse(TASK_ID, "Running");
        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("system")))
                .thenReturn(response);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("Running"));
    }

    @Test
    void createUploadTask_shouldReturnCreatedTask_withCifs() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("CIFS");
        request.setCifsServer("\\\\fileserver\\share");
        request.setCifsShare("models");
        request.setCifsUsername("user");
        request.setCifsPassword("pass");
        request.setWeightType("FP16");

        UploadTaskResponse response = createUploadTaskResponse(TASK_ID, "Running");
        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("system")))
                .thenReturn(response);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("Running"));
    }

    @Test
    void createUploadTask_shouldReturn404_whenModelNotFound() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("PVC");
        request.setSourcePvcName("test-pvc");

        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("system")))
                .thenThrow(new ModelLiteException("0102001", "模型不存在"));

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("模型不存在"));
    }

    @Test
    void createUploadTask_shouldReturn400_whenCifsCredentialsMissing() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("CIFS");
        request.setCifsServer("\\\\fileserver\\share");
        request.setCifsShare("models");

        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("system")))
                .thenThrow(new ModelLiteException("0102042", "CIFS 凭证缺失"));

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(102042))
                .andExpect(jsonPath("$.message").value("CIFS 凭证缺失"));
    }

    @Test
    void createUploadTask_shouldReturnConflict_whenActiveTaskExists() throws Exception {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("PVC");
        request.setSourcePvcName("test-pvc");

        when(uploadApplicationService.createUploadTask(eq(MODEL_ID), any(UploadTaskCreateRequest.class), eq("system")))
                .thenThrow(new ModelLiteException("0102038", "该版本已存在活跃的上传任务"));

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102038))
                .andExpect(jsonPath("$.message").value("该版本已存在活跃的上传任务"));
    }

    @Test
    void getUploadTask_shouldReturnTask() throws Exception {
        UploadTaskResponse response = createUploadTaskResponse(TASK_ID, "Running");
        when(uploadApplicationService.getUploadTask(MODEL_ID, TASK_ID)).thenReturn(response);

        mockMvc.perform(get("/v2/ui/models/{modelId}/upload-tasks/{taskId}", MODEL_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("Running"));
    }

    @Test
    void getUploadTask_shouldReturn404_whenNotFound() throws Exception {
        when(uploadApplicationService.getUploadTask(MODEL_ID, TASK_ID))
                .thenThrow(new ModelLiteException("0102010", "上传任务不存在"));

        mockMvc.perform(get("/v2/ui/models/{modelId}/upload-tasks/{taskId}", MODEL_ID, TASK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102010))
                .andExpect(jsonPath("$.message").value("上传任务不存在"));
    }

    @Test
    void listUploadTasks_shouldReturnList_withStatusFilter() throws Exception {
        UploadTaskResponse response = createUploadTaskResponse(TASK_ID, "Running");
        when(uploadApplicationService.listUploadTasks(MODEL_ID, "Running"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID)
                        .param("status", "Running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("Running"));
    }

    @Test
    void listUploadTasks_shouldReturnEmptyList_whenNoTasks() throws Exception {
        when(uploadApplicationService.listUploadTasks(MODEL_ID, null))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v2/ui/models/{modelId}/upload-tasks", MODEL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void pauseUploadTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/pause", MODEL_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void pauseUploadTask_shouldReturnConflict_whenNotRunning() throws Exception {
        doThrow(new ModelLiteException("0102037", "只能暂停处于运行中的任务"))
                .when(uploadApplicationService).pauseUploadTask(MODEL_ID, TASK_ID);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/pause", MODEL_ID, TASK_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102037))
                .andExpect(jsonPath("$.message").value("只能暂停处于运行中的任务"));
    }

    @Test
    void pauseUploadTask_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ModelLiteException("0102010", "上传任务不存在"))
                .when(uploadApplicationService).pauseUploadTask(MODEL_ID, TASK_ID);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/pause", MODEL_ID, TASK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102010))
                .andExpect(jsonPath("$.message").value("上传任务不存在"));
    }

    @Test
    void resumeUploadTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/resume", MODEL_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void cancelUploadTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/cancel", MODEL_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void cancelUploadTask_shouldReturnConflict_whenAlreadyTerminated() throws Exception {
        doThrow(new ModelLiteException("0102044", "上传任务已终止，无法重复取消"))
                .when(uploadApplicationService).cancelUploadTask(MODEL_ID, TASK_ID);

        mockMvc.perform(post("/v2/ui/models/{modelId}/upload-tasks/{taskId}/cancel", MODEL_ID, TASK_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102044))
                .andExpect(jsonPath("$.message").value("上传任务已终止，无法重复取消"));
    }

    @Test
    void deleteUploadTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v2/ui/models/{modelId}/upload-tasks/{taskId}", MODEL_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void deleteUploadTask_shouldReturnConflict_whenNotTerminal() throws Exception {
        doThrow(new ModelLiteException("0102037", "只能删除处于终态的任务"))
                .when(uploadApplicationService).deleteUploadTask(MODEL_ID, TASK_ID);

        mockMvc.perform(delete("/v2/ui/models/{modelId}/upload-tasks/{taskId}", MODEL_ID, TASK_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102037))
                .andExpect(jsonPath("$.message").value("只能删除处于终态的任务"));
    }

    private UploadTaskResponse createUploadTaskResponse(UUID taskId, String status) {
        UploadTaskResponse response = new UploadTaskResponse();
        response.setTaskId(taskId);
        response.setModelId(MODEL_ID);
        response.setVersionId(VERSION_ID);
        response.setVersionNumber(1);
        response.setSourceType("PVC");
        response.setSourcePath("test-pvc:/models");
        response.setTargetPath(MODEL_ID + "/" + VERSION_ID);
        response.setProgress(50);
        response.setStatus(status);
        response.setErrorMessage(null);
        response.setCreateUser("admin");
        response.setCreateTime(LocalDateTime.now());
        response.setUpdateTime(LocalDateTime.now());
        return response;
    }
}
