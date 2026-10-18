-- DEMO data (dataset de ejemplo, coherente). UUIDs fijos para referencias cross-servicio.
INSERT INTO institutional_structure.macroregion (id, name, code) VALUES
  ('a0000000-0000-0000-0000-000000000001','Regional Antioquia','ANT') ON CONFLICT (id) DO NOTHING;
INSERT INTO institutional_structure.microregion (id, macroregion_id, name, code) VALUES
  ('a0000000-0000-0000-0000-000000000002','a0000000-0000-0000-0000-000000000001','Valle de Aburra','ABU') ON CONFLICT (id) DO NOTHING;
INSERT INTO institutional_structure.department (id, microregion_id, name, dane_code) VALUES
  ('a0000000-0000-0000-0000-000000000003','a0000000-0000-0000-0000-000000000002','Antioquia','05') ON CONFLICT (id) DO NOTHING;
INSERT INTO institutional_structure.municipality (id, department_id, name, dane_code) VALUES
  ('a0000000-0000-0000-0000-000000000004','a0000000-0000-0000-0000-000000000003','Medellin','05001') ON CONFLICT (id) DO NOTHING;
INSERT INTO institutional_structure.training_center (id, municipality_id, center_code, name) VALUES
  ('a0000000-0000-0000-0000-000000000005','a0000000-0000-0000-0000-000000000004','9201','Centro de Comercio y Servicios') ON CONFLICT (id) DO NOTHING;
