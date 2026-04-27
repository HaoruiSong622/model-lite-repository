package com.huawei.modellite.repository.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryRequest;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelTypeResponse;
import com.huawei.modellite.repository.modelweight.application.service.CategoryApplicationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryApi.class)
class CategoryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryApplicationService categoryApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TYPE_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Test
    void getAllCategories_shouldReturnList() throws Exception {
        CategoryResponse response = createCategoryResponse(CATEGORY_ID, "CV");
        when(categoryApplicationService.getAllCategories()).thenReturn(List.of(response));

        mockMvc.perform(get("/v2/ui/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("CV"));
    }

    @Test
    void getCategoryById_shouldReturnCategory() throws Exception {
        CategoryResponse response = createCategoryResponse(CATEGORY_ID, "CV");
        when(categoryApplicationService.getCategoryById(CATEGORY_ID)).thenReturn(response);

        mockMvc.perform(get("/v2/ui/categories/{id}", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("CV"));
    }

    @Test
    void getCategoryById_shouldReturn404_whenNotFound() throws Exception {
        when(categoryApplicationService.getCategoryById(CATEGORY_ID))
                .thenThrow(new ModelLiteException("0101001", "Category not found"));

        mockMvc.perform(get("/v2/ui/categories/{id}", CATEGORY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(101001))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void createCategory_shouldReturnCreatedCategory() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("NLP");
        request.setDescription("Natural Language Processing");

        CategoryResponse response = createCategoryResponse(CATEGORY_ID, "NLP");
        when(categoryApplicationService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v2/ui/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("NLP"));
    }

    @Test
    void createCategory_shouldReturn409_whenDuplicate() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("CV");

        when(categoryApplicationService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new ModelLiteException("0101002", "Category already exists"));

        mockMvc.perform(post("/v2/ui/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(101002))
                .andExpect(jsonPath("$.message").value("Category already exists"));
    }

    @Test
    void deleteCategory_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v2/ui/categories/{id}", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void deleteCategory_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ModelLiteException("0101001", "Category not found"))
                .when(categoryApplicationService).deleteCategory(CATEGORY_ID);

        mockMvc.perform(delete("/v2/ui/categories/{id}", CATEGORY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(101001))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void addModelTypeToCategory_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/v2/ui/categories/{id}/types", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TYPE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void addModelTypeToCategory_shouldReturn404_whenCategoryNotFound() throws Exception {
        doThrow(new ModelLiteException("0101001", "Category not found"))
                .when(categoryApplicationService).addModelTypeToCategory(eq(CATEGORY_ID), any(UUID.class));

        mockMvc.perform(post("/v2/ui/categories/{id}/types", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TYPE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(101001))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void removeModelTypeFromCategory_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v2/ui/categories/{id}/types/{typeId}", CATEGORY_ID, TYPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void removeModelTypeFromCategory_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ModelLiteException("0101001", "Category or type not found"))
                .when(categoryApplicationService).removeModelTypeFromCategory(CATEGORY_ID, TYPE_ID);

        mockMvc.perform(delete("/v2/ui/categories/{id}/types/{typeId}", CATEGORY_ID, TYPE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(101001))
                .andExpect(jsonPath("$.message").value("Category or type not found"));
    }

    private CategoryResponse createCategoryResponse(UUID id, String name) {
        CategoryResponse response = new CategoryResponse();
        response.setId(id);
        response.setName(name);
        response.setDescription("Description of " + name);
        response.setIsBuiltin(false);
        response.setModelTypes(Collections.emptyList());
        response.setCreateTime(Instant.now());
        response.setUpdateTime(Instant.now());
        return response;
    }
}
