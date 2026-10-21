-- Completa el dataset demo de la jerarquía institucional con el último nivel.
INSERT INTO institutional_structure.institutional_unit (id, training_center_id, name, unit_type)
VALUES ('a0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000005', 'Coordinación Académica', 'ACADEMIC')
ON CONFLICT (id) DO NOTHING;
