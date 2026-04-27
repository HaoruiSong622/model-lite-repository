## Patterns Discovered

### Enum Pattern (following TagType.java)
- Enums have `dbValue` and `displayName` fields
- Constructor takes both values
- `getDbValue()` and `getDisplayName()` getters
- `fromDbValue(String)` static method for deserialization, throws IllegalArgumentException on unknown value

### TypeHandler Pattern (following TagTypeTypeHandler.java)
- Extend `BaseTypeHandler<T>`
- Annotate with `@MappedTypes(T.class)`
- `setNonNullParameter` calls `parameter.getDbValue()`
- All `getNullableResult` variants use `rs.getString()` then `T.fromDbValue()` with null check

### MyBatisConfig Registration
- Add new TypeHandler instance to the array passed to `sessionFactory.setTypeHandlers()`
- Wildcard imports handle the class references automatically

### DDL Field Ordering
- Design doc specifies `source_type` comes after `internal_path`, before `nfs_server` and `nfs_path`
- H2 DDL must match the design doc structure since it's the initial creation script

### H2 vs PostgreSQL Migration Strategy
- H2: modify V1 script directly (test environment, recreated each time)
- PostgreSQL: create V2 migration script (production, needs incremental migration)

### GlobalExceptionHandler Pattern
- Use @RestControllerAdvice (not @ControllerAdvice) for REST APIs
- ModelLiteException handler extracts code and message, uses BaseResponse.error(code, message)
- HTTP status code mapping uses suffix-based logic: endsWith("01","06","10","13"...)=404, endsWith("02","08"...)=409, else=400
- Generic Exception returns 500 with code "0105001" and message "内部服务错误"
- BaseResponse.error() parses string code to int, so test assertions use parsed int values (e.g., 102001 not "0102001")

### Domain Service Pattern (ModelDomainService)
- Domain services are plain POJOs (no @Service, no @Transactional) - they only do cross-aggregate validation
- Constructor injection of repository interfaces (CategoryRepository, ModelRepository, TagRepository)
- In tests, use @Mock + MockitoExtension, never inject real repositories
- Domain service throws ModelLiteException with ErrorCode constants for validation failures
- validateModelCreation: category existence → type belongs to category → name uniqueness → resource group capacity (≤100) → global capacity (≤1000)
- validateModelModification: model existence → if categoryId/typeId changed, re-validate category+type+name uniqueness

### Model Aggregate Access from Outside Package
- Model's reconstitution constructor is package-private (not public)
- From test code in different package, must use reflection: getDeclaredConstructor → setAccessible → newInstance
- Same pattern for Category's modelTypes field when injecting ModelType with specific UUID

### ModelRepository Interface
- Created minimal interface with methods needed by ModelDomainService: save, findById, existsByCategoryAndTypeAndName, countByResourceGroup, countAll, update
- Task 15 will expand this with findByIdWithVersions, findAll, findByCondition, etc.

### Category.getModelTypes() for Type Belonging Check
- category.getModelTypes().stream().anyMatch(mt -> mt.getTypeId().equals(typeId)) checks type belongs to category
- Category must be fetched with types loaded (findById vs findByIdWithTypes) - domain service uses findById which may not load types
- IMPORTANT: CategoryRepository.findById may not load model types; need findByIdWithTypes for type belonging check

## FIX-P0 Batch 1 Learnings

### CategoryApi.addModelTypeToCategory Refactoring
- Changed from receiving UUID typeId to receiving Map<String, String> with "name" and "description" keys
- CategoryApplicationService.addModelTypeToCategory now accepts (UUID categoryId, String name, String description)
- Implementation fetches category by ID with types, calls category.addModelType(name, description), then saves
- Category.addModelType() already handles name uniqueness validation (MODEL_TYPE_NAME_EXISTS)

### TagApi.addTagToModel Refactoring
- Changed from receiving single UUID tagId to receiving List<UUID> tagIds
- TagApplicationService.addTagToModel now accepts (UUID modelId, List<UUID> tagIds)
- Implementation validates tag limit first, then validates all tags exist, then adds all tags

### Tag Limit Validation (≤20)
- Error code: MODEL_TAG_LIMIT_EXCEEDED = "0102025"
- Validation: existingTags.size() + tagIds.size() > 20 → throw ModelLiteException
- Must check limit BEFORE validating individual tags (to avoid unnecessary DB calls when limit exceeded)
- Must check limit BEFORE adding any tags (atomic validation)

### Test Update Patterns
- CategoryApiTest: Changed request body from UUID to Map with name/description
- TagApiTest: Changed request body from single UUID to List<UUID>
- CategoryApplicationServiceTest: Added new nested class AddModelTypeToCategoryTests
- TagApplicationServiceTest: Updated AddTagToModelTests to use List<UUID>, added tag limit exceeded test
- Mockito strict stubbing: when limit check throws first, don't stub findById (unnecessary stubbing error)

### Pre-existing Test Failure
- HealthEndpointTest.should_notExposeSensitiveEndpoints fails with 500 instead of 404 (unrelated to our changes)
- This is a pre-existing environment issue, not caused by our modifications
