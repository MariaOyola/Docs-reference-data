ALTER TABLE institutional_structure.microregion
ADD CONSTRAINT fk_microregion_macroregion
FOREIGN KEY (macroregion_id)
REFERENCES institutional_structure.macroregion(id)
ON UPDATE CASCADE
ON DELETE RESTRICT;

ALTER TABLE institutional_structure.department
ADD CONSTRAINT fk_department_microregion
FOREIGN KEY (microregion_id)
REFERENCES institutional_structure.microregion(id)
ON UPDATE CASCADE
ON DELETE RESTRICT;

ALTER TABLE institutional_structure.municipality
ADD CONSTRAINT fk_municipality_department
FOREIGN KEY (department_id)
REFERENCES institutional_structure.department(id)
ON UPDATE CASCADE
ON DELETE RESTRICT;

ALTER TABLE institutional_structure.training_center
ADD CONSTRAINT fk_training_center_municipality
FOREIGN KEY (municipality_id)
REFERENCES institutional_structure.municipality(id)
ON UPDATE CASCADE
ON DELETE RESTRICT;

ALTER TABLE institutional_structure.institutional_unit
ADD CONSTRAINT fk_institutional_unit_training_center
FOREIGN KEY (training_center_id)
REFERENCES institutional_structure.training_center(id)
ON UPDATE CASCADE
ON DELETE CASCADE;

ALTER TABLE parameterization.catalog_detail
ADD CONSTRAINT fk_catalog_detail_catalog
FOREIGN KEY (catalog_id)
REFERENCES parameterization.catalog(id)
ON UPDATE CASCADE
ON DELETE CASCADE;