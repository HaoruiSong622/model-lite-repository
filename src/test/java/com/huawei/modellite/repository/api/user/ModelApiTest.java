package com.huawei.modellite.repository.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.ModelCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelListResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelModifyRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelResponse;
import com.huawei.modellite.repository.modelweight.application.dto.VersionCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.application.service.ModelApplicationService;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelApi.class)
class ModelApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelApplicationService modelApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID MODEL_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID VERSION_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID CATEGORY_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
    private static final UUID TYPE_ID = UUID.fromString("880e8400-e29b-41d4-a716-446655440003");

    @Test
    void createModel_shouldReturnCreatedModel() throws Exception {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName("TestModel");
        request.setDescription("Test description");
        request.setCategoryId(CATEGORY_ID);
        request.setTypeId(TYPE_ID);
        request.setResourceGroup("default");

        ModelResponse response = createModelResponse(MODEL_ID, "TestModel");
        when(modelApplicationService.createModel(any(ModelCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v2/ui/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(MODEL_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("TestModel"));
    }

    @Test
    void createModel_shouldReturnConflict_whenNameExists() throws Exception {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName("ExistingModel");

        when(modelApplicationService.createModel(any(ModelCreateRequest.class)))
                .thenThrow(new ModelLiteException("0102002", "Model name already exists"));

        mockMvc.perform(post("/v2/ui/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102002))
                .andExpect(jsonPath("$.message").value("Model name already exists"));
    }

    @Test
    void getModel_shouldReturnModel() throws Exception {
        ModelResponse response = createModelResponse(MODEL_ID, "TestModel");
        when(modelApplicationService.getModel(MODEL_ID)).thenReturn(response);

        mockMvc.perform(get("/v2/ui/models/{id}", MODEL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(MODEL_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("TestModel"));
    }

    @Test
    void getModel_shouldReturn404_whenNotFound() throws Exception {
        when(modelApplicationService.getModel(MODEL_ID))
                .thenThrow(new ModelLiteException("0102001", "Model not found"));

        mockMvc.perform(get("/v2/ui/models/{id}", MODEL_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("Model not found"));
    }

    @Test
    void modifyModel_shouldReturnModifiedModel() throws Exception {
        ModelModifyRequest request = new ModelModifyRequest();
        request.setDescription("Updated description");
        request.setCategoryId(CATEGORY_ID);
        request.setTypeId(TYPE_ID);

        ModelResponse response = createModelResponse(MODEL_ID, "TestModel");
        response.setDescription("Updated description");
        when(modelApplicationService.modifyModel(eq(MODEL_ID), any(ModelModifyRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/v2/ui/models/{id}", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(MODEL_ID.toString()))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }

    @Test
    void modifyModel_shouldReturn404_whenNotFound() throws Exception {
        ModelModifyRequest request = new ModelModifyRequest();
        request.setDescription("Updated description");

        when(modelApplicationService.modifyModel(eq(MODEL_ID), any(ModelModifyRequest.class)))
                .thenThrow(new ModelLiteException("0102001", "Model not found"));

        mockMvc.perform(patch("/v2/ui/models/{id}", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("Model not found"));
    }

    @Test
    void listModels_shouldReturnPageResult() throws Exception {
        ModelListResponse modelListResponse = new ModelListResponse();
        modelListResponse.setId(MODEL_ID);
        modelListResponse.setName("TestModel");

        PageResult<ModelListResponse> pageResult = new PageResult<>();
        pageResult.setItems(List.of(modelListResponse));
        pageResult.setTotal(1);
        pageResult.setPage(0);
        pageResult.setPageSize(10);
        pageResult.setTotalPages(1);

        when(modelApplicationService.listModels(any(ModelQueryCondition.class))).thenReturn(pageResult);

        mockMvc.perform(get("/v2/ui/models")
                        .param("categoryId", CATEGORY_ID.toString())
                        .param("typeId", TYPE_ID.toString())
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].id").value(MODEL_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("TestModel"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void createVersion_shouldReturnCreatedVersion() throws Exception {
        VersionCreateRequest request = new VersionCreateRequest();
        request.setSourceType("PVC");
        request.setPvcName("test-pvc");
        request.setInternalPath("/models");
        request.setWeightType("FP32");

        VersionResponse response = createVersionResponse(VERSION_ID, 1);
        when(modelApplicationService.createVersion(eq(MODEL_ID), any(VersionCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v2/ui/models/{id}/versions", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$.data.versionNumber").value(1));
    }

    @Test
    void createVersion_shouldReturn404_whenModelNotFound() throws Exception {
        VersionCreateRequest request = new VersionCreateRequest();
        request.setSourceType("PVC");
        request.setPvcName("test-pvc");

        when(modelApplicationService.createVersion(eq(MODEL_ID), any(VersionCreateRequest.class)))
                .thenThrow(new ModelLiteException("0102001", "Model not found"));

        mockMvc.perform(post("/v2/ui/models/{id}/versions", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("Model not found"));
    }

    @Test
    void getVersion_shouldReturnVersion() throws Exception {
        VersionResponse response = createVersionResponse(VERSION_ID, 1);
        when(modelApplicationService.getVersion(MODEL_ID, VERSION_ID)).thenReturn(response);

        mockMvc.perform(get("/v2/ui/models/{id}/versions/{versionId}", MODEL_ID, VERSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$.data.versionNumber").value(1));
    }

    @Test
    void getVersion_shouldReturn404_whenNotFound() throws Exception {
        when(modelApplicationService.getVersion(MODEL_ID, VERSION_ID))
                .thenThrow(new ModelLiteException("0102006", "Version not found"));

        mockMvc.perform(get("/v2/ui/models/{id}/versions/{versionId}", MODEL_ID, VERSION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102006))
                .andExpect(jsonPath("$.message").value("Version not found"));
    }

    private ModelResponse createModelResponse(UUID id, String name) {
        ModelResponse response = new ModelResponse();
        response.setId(id);
        response.setName(name);
        response.setDescription("Description of " + name);
        response.setCategoryId(CATEGORY_ID);
        response.setTypeId(TYPE_ID);
        response.setResourceGroup("default");
        response.setCreateUser("admin");
        response.setAuthor("author");
        response.setSeriesName("series");
        response.setVersions(Collections.emptyList());
        response.setTags(Collections.emptyList());
        response.setCreateTime(Instant.now());
        response.setUpdateTime(Instant.now());
        return response;
    }

    private VersionResponse createVersionResponse(UUID id, int versionNumber) {
        VersionResponse response = new VersionResponse();
        response.setId(id);
        response.setModelId(MODEL_ID);
        response.setVersionNumber(versionNumber);
        response.setStatus("AVAILABLE");
        response.setIsRegistered(false);
        response.setIsLocked(false);
        response.setSourceType("PVC");
        response.setPvcName("test-pvc");
        response.setInternalPath("/models");
        response.setWeightType("FP32");
        response.setCreateTime(Instant.now());
        response.setUpdateTime(Instant.now());
        return response;
    }
}
