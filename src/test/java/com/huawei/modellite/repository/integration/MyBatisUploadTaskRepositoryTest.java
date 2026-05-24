package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.infrastructure.persistence.MyBatisUploadTaskRepository;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(K8sJobServiceTestConfig.class)
class MyBatisUploadTaskRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private MyBatisUploadTaskRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KubernetesClient kubernetesClient;

    private UUID categoryId;
    private UUID typeId;
    private UUID modelId;
    private UUID versionId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM upload_task");
        jdbcTemplate.execute("DELETE FROM model_version");
        jdbcTemplate.execute("DELETE FROM model");
        jdbcTemplate.execute("DELETE FROM model_type");
        jdbcTemplate.execute("DELETE FROM category");

        categoryId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        modelId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        String categoryName = "TestCategory-" + categoryId;
        String typeName = "TestType-" + typeId;
        String modelName = "TestModel-" + modelId;

        jdbcTemplate.execute(String.format(
            "INSERT INTO category (id, name) VALUES ('%s', '%s')", categoryId, categoryName));
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_type (id, category_id, name) VALUES ('%s', '%s', '%s')", typeId, categoryId, typeName));
        jdbcTemplate.execute(String.format(
            "INSERT INTO model (id, name, category_id, type_id, resource_group, create_user) " +
            "VALUES ('%s', '%s', '%s', '%s', 'test-rg', 'test-user')", modelId, modelName, categoryId, typeId));
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_version (id, model_id, version_number, status) " +
            "VALUES ('%s', '%s', 1, 'NoWeight')", versionId, modelId));
    }

    @Test
    @DisplayName("should save and find upload task by id")
    void should_save_and_findById() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("nfs-server", "/nfs/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/target", "user");

        repository.save(task);

        Optional<UploadTask> found = repository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(taskId, found.get().getTaskId());
        assertEquals(modelId, found.get().getModelId());
        assertEquals(versionId, found.get().getVersionId());
        assertEquals(SourceType.NFS, found.get().getSourcePath().getSourceType());
        assertEquals("nfs-server:/nfs/path", found.get().getSourcePath().getPath());
        assertNull(found.get().getSourcePath().getCredentials());
        assertEquals("/target", found.get().getTargetPath());
        assertEquals(0, found.get().getProgress());
        assertEquals(TaskStatus.PENDING, found.get().getStatus());
        assertEquals("user", found.get().getCreateUser());
        assertEquals(0L, found.get().getVersion());
    }

    @Test
    @DisplayName("should save and read back CIFS mode upload task with credentials")
    void should_save_and_readBack_cifs_task() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofCifs("cifs-server", "share", "admin", "secret");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/target/cifs", "cifs-user");

        repository.save(task);

        Optional<UploadTask> found = repository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(SourceType.CIFS, found.get().getSourcePath().getSourceType());
        assertEquals("//cifs-server/share", found.get().getSourcePath().getPath());
        assertNotNull(found.get().getSourcePath().getCredentials());
        assertEquals("admin", found.get().getSourcePath().getCredentials().getUsername());
        assertEquals("secret", found.get().getSourcePath().getCredentials().getPassword());
    }

    @Test
    @DisplayName("should find upload tasks by model id ordered by create time desc")
    void should_findByModelId() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofPvc("pvc-1", "/internal");

        UploadTask task1 = UploadTask.createUploadTask(taskId1, modelId, versionId, sourcePath, "/t1", "user");
        UploadTask task2 = UploadTask.createUploadTask(taskId2, modelId, versionId, sourcePath, "/t2", "user");

        repository.save(task1);
        repository.save(task2);

        List<UploadTask> tasks = repository.findByModelId(modelId);
        assertEquals(2, tasks.size());
        assertEquals(taskId2, tasks.get(0).getTaskId());
        assertEquals(taskId1, tasks.get(1).getTaskId());
    }

    @Test
    @DisplayName("should find upload tasks by version id")
    void should_findByVersionId() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");

        repository.save(task);

        List<UploadTask> tasks = repository.findByVersionId(versionId);
        assertEquals(1, tasks.size());
        assertEquals(taskId, tasks.get(0).getTaskId());
    }

    @Test
    @DisplayName("should find active task by version id when exists")
    void should_findActiveByVersionId_when_exists() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");

        repository.save(task);

        Optional<UploadTask> found = repository.findActiveByVersionId(versionId);
        assertTrue(found.isPresent());
        assertEquals(taskId, found.get().getTaskId());
    }

    @Test
    @DisplayName("should return empty when no active task by version id")
    void should_returnEmpty_findActiveByVersionId_when_notExists() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");
        setField(task, "status", TaskStatus.COMPLETED);

        repository.save(task);

        Optional<UploadTask> found = repository.findActiveByVersionId(versionId);
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("should find upload tasks by status")
    void should_findByStatus() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");

        UploadTask task1 = UploadTask.createUploadTask(taskId1, modelId, versionId, sourcePath, "/t1", "user");
        setField(task1, "status", TaskStatus.RUNNING);

        UploadTask task2 = UploadTask.createUploadTask(taskId2, modelId, versionId, sourcePath, "/t2", "user");
        setField(task2, "status", TaskStatus.FAILED);

        repository.save(task1);
        repository.save(task2);

        List<UploadTask> runningTasks = repository.findByStatus(TaskStatus.RUNNING);
        assertEquals(1, runningTasks.size());
        assertEquals(taskId1, runningTasks.get(0).getTaskId());
    }

    @Test
    @DisplayName("should find upload tasks by status in list")
    void should_findByStatusIn() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        UUID taskId3 = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");

        UploadTask task1 = UploadTask.createUploadTask(taskId1, modelId, versionId, sourcePath, "/t1", "user");
        setField(task1, "status", TaskStatus.PENDING);

        UploadTask task2 = UploadTask.createUploadTask(taskId2, modelId, versionId, sourcePath, "/t2", "user");
        setField(task2, "status", TaskStatus.RUNNING);

        UploadTask task3 = UploadTask.createUploadTask(taskId3, modelId, versionId, sourcePath, "/t3", "user");
        setField(task3, "status", TaskStatus.COMPLETED);

        repository.save(task1);
        repository.save(task2);
        repository.save(task3);

        List<UploadTask> tasks = repository.findByStatusIn(Arrays.asList(TaskStatus.PENDING, TaskStatus.RUNNING));
        assertEquals(2, tasks.size());
    }

    @Test
    @DisplayName("should update task and increment version")
    void should_update_and_increment_version() throws Exception {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");
        repository.save(task);

        setField(task, "status", TaskStatus.RUNNING);
        setField(task, "updateTime", LocalDateTime.now());

        repository.update(task);

        Optional<UploadTask> found = repository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(TaskStatus.RUNNING, found.get().getStatus());
        assertEquals(1L, found.get().getVersion());
    }

    @Test
    @DisplayName("should throw exception on optimistic lock conflict with two concurrent updates")
    void should_throw_on_optimistic_lock_conflict() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");
        repository.save(task);

        // First concurrent update succeeds (db version: 0 -> 1)
        setField(task, "status", TaskStatus.RUNNING);
        setField(task, "updateTime", LocalDateTime.now());
        repository.update(task);

        // Second concurrent update with stale version (still 0 in memory) fails
        setField(task, "status", TaskStatus.COMPLETED);
        setField(task, "updateTime", LocalDateTime.now());

        ModelLiteException exception = assertThrows(ModelLiteException.class, () -> repository.update(task));
        assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, exception.getCode());
        assertEquals("任务状态已被其他操作更新", exception.getMessage());
    }

    @Test
    @DisplayName("should update progress only")
    void should_updateProgress() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");
        repository.save(task);

        setField(task, "progress", 50);
        setField(task, "updateTime", LocalDateTime.now());

        repository.updateProgress(task);

        Optional<UploadTask> found = repository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(50, found.get().getProgress());
    }

    @Test
    @DisplayName("should delete task by id")
    void should_deleteById() {
        UUID taskId = UUID.randomUUID();
        SourcePath sourcePath = SourcePath.ofNfs("srv", "/path");
        UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/t", "user");
        repository.save(task);

        assertTrue(repository.findById(taskId).isPresent());

        repository.deleteById(taskId);

        assertTrue(repository.findById(taskId).isEmpty());
    }

    @Test
    @DisplayName("should return empty when find by non-existent id")
    void should_returnEmpty_when_findById_notExists() {
        Optional<UploadTask> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
