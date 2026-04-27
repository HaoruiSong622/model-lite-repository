-- Disable foreign key checks for H2
SET REFERENTIAL_INTEGRITY FALSE;

-- Truncate all tables in reverse dependency order
TRUNCATE TABLE model_tag;
TRUNCATE TABLE model_version;
TRUNCATE TABLE model;
TRUNCATE TABLE model_type_tag;
TRUNCATE TABLE model_type;
TRUNCATE TABLE tag;
TRUNCATE TABLE category;

-- Re-enable foreign key checks
SET REFERENTIAL_INTEGRITY TRUE;
