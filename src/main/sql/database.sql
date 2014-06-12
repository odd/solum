CREATE DATABASE solum;
CREATE TABLE solum.counter (name VARCHAR(255) PRIMARY KEY, amount INT NOT NULL DEFAULT 0);
CREATE USER 'solum'@'localhost' IDENTIFIED BY 'solum';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON solum.* TO 'solum'@'localhost';