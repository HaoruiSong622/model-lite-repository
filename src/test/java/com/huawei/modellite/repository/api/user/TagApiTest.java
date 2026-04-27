package com.huawei.modellite.repository.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.TagRequest;
import com.huawei.modellite.repository.modelweight.application.dto.TagResponse;
import com.huawei.modellite.repository.modelweight.application.service.TagApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

@WebMvcTest(TagApi.class)
class TagApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagApplicationService tagApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID TAG_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID MODEL_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Test
    void getAllTags_shouldReturnList() throws Exception {
        TagResponse response = createTagResponse(TAG_ID, "Production");
        when(tagApplicationService.getAllTags()).thenReturn(List.of(response));

        mockMvc.perform(get("/v2/ui/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(TAG_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Production"));
    }

    @Test
    void createTag_shouldReturnCreatedTag() throws Exception {
        TagRequest request = new TagRequest();
        request.setName("Beta");

        TagResponse response = createTagResponse(TAG_ID, "Beta");
        when(tagApplicationService.createTag(any(TagRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v2/ui/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(TAG_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Beta"));
    }

    @Test
    void createTag_shouldReturn409_whenDuplicate() throws Exception {
        TagRequest request = new TagRequest();
        request.setName("Production");

        when(tagApplicationService.createTag(any(TagRequest.class)))
                .thenThrow(new ModelLiteException("0102002", "Tag already exists"));

        mockMvc.perform(post("/v2/ui/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102002))
                .andExpect(jsonPath("$.message").value("Tag already exists"));
    }

    @Test
    void deleteTag_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v2/ui/tags/{id}", TAG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void deleteTag_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ModelLiteException("0102001", "Tag not found"))
                .when(tagApplicationService).deleteTag(TAG_ID);

        mockMvc.perform(delete("/v2/ui/tags/{id}", TAG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(102001))
                .andExpect(jsonPath("$.message").value("Tag not found"));
    }

    @Test
    void addTagToModel_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/v2/ui/models/{modelId}/tags", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(TAG_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void addTagToModel_shouldReturn404_whenModelNotFound() throws Exception {
        doThrow(new ModelLiteException("0103001", "Model not found"))
                .when(tagApplicationService).addTagToModel(eq(MODEL_ID), any(List.class));

        mockMvc.perform(post("/v2/ui/models/{modelId}/tags", MODEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(TAG_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(103001))
                .andExpect(jsonPath("$.message").value("Model not found"));
    }

    @Test
    void removeTagFromModel_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v2/ui/models/{modelId}/tags/{tagId}", MODEL_ID, TAG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void removeTagFromModel_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ModelLiteException("0103001", "Model or tag not found"))
                .when(tagApplicationService).removeTagFromModel(MODEL_ID, TAG_ID);

        mockMvc.perform(delete("/v2/ui/models/{modelId}/tags/{tagId}", MODEL_ID, TAG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(103001))
                .andExpect(jsonPath("$.message").value("Model or tag not found"));
    }

    private TagResponse createTagResponse(UUID id, String name) {
        TagResponse response = new TagResponse();
        response.setId(id);
        response.setName(name);
        response.setTagType("custom");
        response.setIsBuiltin(false);
        response.setCreateTime(Instant.now());
        response.setUpdateTime(Instant.now());
        return response;
    }
}
