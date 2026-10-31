-- Apex Banking Multi-Database Initialization Script
CREATE DATABASE IF NOT EXISTS apex_account_db;
CREATE DATABASE IF NOT EXISTS apex_transaction_db;
CREATE DATABASE IF NOT EXISTS apex_fraud_db;
CREATE DATABASE IF NOT EXISTS apex_notification_db;

-- Create apex_user for all host scopes (Docker bridge, localhost, and 127.0.0.1)
CREATE USER IF NOT EXISTS 'apex_user'@'%' IDENTIFIED BY 'apex_secret';
CREATE USER IF NOT EXISTS 'apex_user'@'localhost' IDENTIFIED BY 'apex_secret';
CREATE USER IF NOT EXISTS 'apex_user'@'127.0.0.1' IDENTIFIED BY 'apex_secret';

-- Grant full permissions on all banking databases
GRANT ALL PRIVILEGES ON apex_account_db.* TO 'apex_user'@'%';
GRANT ALL PRIVILEGES ON apex_account_db.* TO 'apex_user'@'localhost';
GRANT ALL PRIVILEGES ON apex_account_db.* TO 'apex_user'@'127.0.0.1';

GRANT ALL PRIVILEGES ON apex_transaction_db.* TO 'apex_user'@'%';
GRANT ALL PRIVILEGES ON apex_transaction_db.* TO 'apex_user'@'localhost';
GRANT ALL PRIVILEGES ON apex_transaction_db.* TO 'apex_user'@'127.0.0.1';

GRANT ALL PRIVILEGES ON apex_fraud_db.* TO 'apex_user'@'%';
GRANT ALL PRIVILEGES ON apex_fraud_db.* TO 'apex_user'@'localhost';
GRANT ALL PRIVILEGES ON apex_fraud_db.* TO 'apex_user'@'127.0.0.1';

GRANT ALL PRIVILEGES ON apex_notification_db.* TO 'apex_user'@'%';
GRANT ALL PRIVILEGES ON apex_notification_db.* TO 'apex_user'@'localhost';
GRANT ALL PRIVILEGES ON apex_notification_db.* TO 'apex_user'@'127.0.0.1';

FLUSH PRIVILEGES;
