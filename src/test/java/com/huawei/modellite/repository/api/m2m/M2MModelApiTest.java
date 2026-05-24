package com.huawei.modellite.repository.api.m2m;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.weighttask.application.dto.ArchiveRequest;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(M2MModelApi.class)
class M2MModelApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadApplicationService uploadApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID MODEL_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID VERSION_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Test
    void archiveTrainingWeight_shouldReturnArchivedVersion() throws Exception {
        ArchiveRequest request = new ArchiveRequest();
        request.setSourceType("PVC");
        request.setPvcName("training-pvc");
        request.setInternalPath("/output/weights");
        request.setWeightType("FP32");

        TrainingMetadataDto metadata = new TrainingMetadataDto();
        metadata.setTrainFrame("PyTorch");
        metadata.setTrainType("LLM");
        metadata.setFinalLoss("0.01");
        request.setTrainingMetadata(metadata);

        VersionResponse response = createVersionResponse(VERSION_ID, 1);
        when(uploadApplicationService.archiveTrainingWeight(eq(MODEL_ID), any(ArchiveRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v2/models/{modelId}/versions/archive", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.sourceType").value("PVC"))
                .andExpect(jsonPath("$.data.weightType").value("FP32"));
    }

    @Test
    void archiveTrainingWeight_shouldReturn404_whenModelNotFound() throws Exception {
        ArchiveRequest request = new ArchiveRequest();
        request.setSourceType("PVC");
        request.setPvcName("pvc");
        request.setInternalPath("/path");

        when(uploadApplicationService.archiveTrainingWeight(eq(MODEL_ID), any(ArchiveRequest.class)))
                .thenThrow(new ModelLiteException("0102001", "Model not found"));

        mockMvc.perform(post("/v2/models/{modelId}/versions/archive", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("Model not found"));
    }

    @Test
    void archiveTrainingWeight_shouldReturn400_whenInvalidSourceType() throws Exception {
        ArchiveRequest request = new ArchiveRequest();
        request.setSourceType("INVALID");

        when(uploadApplicationService.archiveTrainingWeight(eq(MODEL_ID), any(ArchiveRequest.class)))
                .thenThrow(new ModelLiteException("0104003", "Invalid source type"));

        mockMvc.perform(post("/v2/models/{modelId}/versions/archive", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(104003))
                .andExpect(jsonPath("$.message").value("Invalid source type"));
    }

    @Test
    void archiveTrainingWeight_shouldReturn400_whenVersionCapacityExceeded() throws Exception {
        ArchiveRequest request = new ArchiveRequest();
        request.setSourceType("PVC");
        request.setPvcName("training-pvc");
        request.setInternalPath("/output/weights");
        request.setWeightType("FP32");

        when(uploadApplicationService.archiveTrainingWeight(eq(MODEL_ID), any(ArchiveRequest.class)))
                .thenThrow(new ModelLiteException("0102009", "版本数量超出上限"));

        mockMvc.perform(post("/v2/models/{modelId}/versions/archive", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(102009))
                .andExpect(jsonPath("$.message").value("版本数量超出上限"));
    }

    private VersionResponse createVersionResponse(UUID id, int versionNumber) {
        VersionResponse response = new VersionResponse();
        response.setId(id);
        response.setModelId(MODEL_ID);
        response.setVersionNumber(versionNumber);
        response.setStatus("AVAILABLE");
        response.setRegistered(true);
        response.setLocked(false);
        response.setSourceType("PVC");
        response.setPvcName("training-pvc");
        response.setInternalPath("/output/weights");
        response.setWeightType("FP32");
        response.setCreateTime(Instant.now());
        response.setUpdateTime(Instant.now());
        return response;
    }
}
