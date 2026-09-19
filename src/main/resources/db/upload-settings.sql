-- Run against the main database (cxfdemo1). Existing values are preserved.
-- This directory is for HTTP uploads saved on the application server, not FTP/TFTP.
-- Change the directory for other deployments before applying this script.
INSERT INTO system_properties (prop_key, prop_value)
VALUES ('file.upload.dir', 'E:/antigravity_workspace/ssm-cxf-webservice-demo/uploads'),
       ('file.upload.max-size', '10485760')
ON DUPLICATE KEY UPDATE prop_value = system_properties.prop_value;
