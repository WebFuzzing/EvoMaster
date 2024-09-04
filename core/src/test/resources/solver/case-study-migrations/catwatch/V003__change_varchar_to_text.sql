ALTER TABLE contributor ALTER COLUMN name TYPE text;
ALTER TABLE contributor ALTER COLUMN organization_name TYPE text;
ALTER TABLE contributor ALTER COLUMN url TYPE text;

ALTER TABLE project ALTER COLUMN description TYPE text;
ALTER TABLE project ALTER COLUMN last_pushed TYPE text;
ALTER TABLE project ALTER COLUMN name TYPE text;
ALTER TABLE project ALTER COLUMN organization_name TYPE text;
ALTER TABLE project ALTER COLUMN primary_language TYPE text;
ALTER TABLE project ALTER COLUMN url TYPE text;

ALTER TABLE statistics ALTER COLUMN organization_name TYPE text;

ALTER TABLE language_list ALTER COLUMN language TYPE text;
