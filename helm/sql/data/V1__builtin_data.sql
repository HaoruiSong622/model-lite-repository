-- ============================================================
-- V1__builtin_data.sql
-- ModelLite Repository - Builtin data (PostgreSQL)
-- ============================================================

-- Builtin Categories
INSERT INTO category (id, name, description, is_builtin, create_time, update_time) VALUES
('10000000-0000-0000-0000-000000000001', 'TextGeneration', 'Text generation models including LLMs and embedding models', TRUE, NOW(), NOW()),
('10000000-0000-0000-0000-000000000002', 'ImageTextToText', 'Vision-language models that process both images and text', TRUE, NOW(), NOW()),
('10000000-0000-0000-0000-000000000003', 'ImageGeneration', 'Image generation models including diffusion models', TRUE, NOW(), NOW());

-- Builtin Model Types for TextGeneration
INSERT INTO model_type (id, category_id, name, description, is_builtin, create_time, update_time) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'LLM', 'Large Language Models for text generation and reasoning', TRUE, NOW(), NOW()),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Embedding', 'Text embedding models for semantic search and retrieval', TRUE, NOW(), NOW());

-- Builtin Model Types for ImageTextToText
INSERT INTO model_type (id, category_id, name, description, is_builtin, create_time, update_time) VALUES
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'VLM', 'Vision Language Models for image understanding and description', TRUE, NOW(), NOW()),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'LMVLM', 'Large Multimodal Vision Language Models for complex visual reasoning', TRUE, NOW(), NOW());

-- Builtin Model Types for ImageGeneration
INSERT INTO model_type (id, category_id, name, description, is_builtin, create_time, update_time) VALUES
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', 'Diffusion', 'Diffusion models for high-quality image generation', TRUE, NOW(), NOW());

-- Builtin Tags
INSERT INTO tag (id, name, tag_type, is_builtin, create_time, update_time) VALUES
('30000000-0000-0000-0000-000000000001', 'supportFinetune', 'Capability', TRUE, NOW(), NOW()),
('30000000-0000-0000-0000-000000000002', 'vision', 'Capability', TRUE, NOW(), NOW()),
('30000000-0000-0000-0000-000000000003', 'multimodal', 'Capability', TRUE, NOW(), NOW());