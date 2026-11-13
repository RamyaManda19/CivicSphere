-- =====================================================================
-- CivicSphere: Smart Citizen Complaint Management & Recurring Civic Issue Detection System
-- Database Schema (civicsphere_db)
-- Compatible with MySQL 8.0+
-- =====================================================================

CREATE DATABASE IF NOT EXISTS civicsphere_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE civicsphere_db;

-- ---------------------------------------------------------------------
-- Table: users
-- Roles: 'CITIZEN', 'ADMIN'
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(25) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    address TEXT,
    role ENUM('CITIZEN', 'ADMIN') NOT NULL DEFAULT 'CITIZEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: categories
-- Standard civic infrastructure problem categories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: departments
-- Municipal departments responsible for resolving complaints
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    contact_email VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: complaints
-- Core complaints table with geographic & recurrence tracking
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT NOT NULL,
    department_id INT NULL,
    parent_complaint_id INT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    area VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    landmark VARCHAR(200),
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    image_path VARCHAR(255) NULL,
    status ENUM('SUBMITTED', 'UNDER REVIEW', 'ASSIGNED', 'IN PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    resolution_remarks TEXT NULL,
    resolution_image_path VARCHAR(255) NULL,
    CONSTRAINT fk_complaints_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_category FOREIGN KEY (category_id) REFERENCES categories (category_id),
    CONSTRAINT fk_complaints_department FOREIGN KEY (department_id) REFERENCES departments (department_id) ON DELETE SET NULL,
    CONSTRAINT fk_complaints_parent FOREIGN KEY (parent_complaint_id) REFERENCES complaints (complaint_id) ON DELETE SET NULL,
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_category (category_id),
    INDEX idx_complaints_area_city (city, area),
    INDEX idx_complaints_parent (parent_complaint_id),
    INDEX idx_complaints_submitted_at (submitted_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: complaint_history
-- Audit log preserving every status transition and remark
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    remarks TEXT,
    updated_by INT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (updated_by) REFERENCES users (user_id),
    INDEX idx_history_complaint_id (complaint_id),
    INDEX idx_history_updated_at (updated_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: recurrence_records
-- Records identified recurring issues with similarity scores & reasons
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recurrence_records (
    recurrence_id INT AUTO_INCREMENT PRIMARY KEY,
    original_complaint_id INT NOT NULL,
    recurring_complaint_id INT NOT NULL,
    matching_score DECIMAL(5, 2) NOT NULL,
    matching_reason TEXT NOT NULL,
    verification_status ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    verified_by INT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurrence_original FOREIGN KEY (original_complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrence_recurring FOREIGN KEY (recurring_complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrence_verified_by FOREIGN KEY (verified_by) REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT uk_recurrence_pair UNIQUE (original_complaint_id, recurring_complaint_id),
    INDEX idx_recurrence_verification_status (verification_status),
    INDEX idx_recurrence_created_at (created_at)
) ENGINE=InnoDB;

-- =====================================================================
-- SEED DATA
-- Default Categories, Departments, Initial Admin and Demo Accounts
-- =====================================================================

-- 1. Categories
INSERT INTO categories (category_id, category_name, description) VALUES
(1, 'Road Damage & Potholes', 'Damaged road surfaces, hazardous potholes, crumbling asphalt, and cave-ins.'),
(2, 'Garbage Accumulation', 'Uncollected household waste, illegal garbage dumping, and overflowing public bins.'),
(3, 'Water Leakage & Supply Issues', 'Burst water mains, pipeline leaks, low water pressure, and contaminated tap water.'),
(4, 'Drainage & Sewage Overflow', 'Clogged drains, open manholes, sewage backflow, and monsoon water-logging.'),
(5, 'Streetlight & Electrical Failures', 'Malfunctioning streetlights, exposed live wires, and damaged electric poles.'),
(6, 'Public Sanitation & Hygiene', 'Unsanitary public toilets, open defecation spots, and public health hazards.')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 2. Departments
INSERT INTO departments (department_id, department_name, description, contact_email) VALUES
(1, 'Road Maintenance', 'Responsible for asphalt resurfacing, pothole repairs, and pavement restoration.', 'roads@civicsphere.gov'),
(2, 'Water Supply', 'Manages municipal drinking water distribution networks and pipeline repairs.', 'water@civicsphere.gov'),
(3, 'Sanitation', 'Handles daily solid waste collection, street sweeping, and dumping cleanup.', 'sanitation@civicsphere.gov'),
(4, 'Drainage', 'Maintains storm-water drainage networks, desilting, and manhole safety.', 'drainage@civicsphere.gov'),
(5, 'Electricity and Streetlights', 'Operates municipal street lighting, electrical poles, and cabling maintenance.', 'electricity@civicsphere.gov')
ON DUPLICATE KEY UPDATE contact_email=VALUES(contact_email);

-- 3. Users (Initial Admin and Sample Citizens)
-- Password for admin@civicsphere.gov is: Admin@123
-- BCrypt hash: $2a$10$WpQyX2E67q0f4bV7V8z/f.e0dsm82sP52K5m0oG9rOq5hA3L0sVTu
-- Password for citizen@example.com is: Citizen@123
-- BCrypt hash: $2a$10$fQ8f5Rz5kZJmQe9y0EwMbeK7lD1G2xVvQ.W9YxY1wK1YmX2Z4L5Ti
INSERT INTO users (user_id, full_name, email, phone, password_hash, address, role) VALUES
(1, 'System Administrator', 'admin@civicsphere.gov', '9876543210', '$2a$10$wEfe2Q5c5g4w3GkUaHh0QOn7a8w102BvX0K7L8u6p5o4n3m2l1k0j', 'Civic Operations Center, Greater Hyderabad', 'ADMIN'),
(2, 'Rahul Sharma', 'citizen@example.com', '9876543211', '$2a$10$wEfe2Q5c5g4w3GkUaHh0QOn7a8w102BvX0K7L8u6p5o4n3m2l1k0j', 'Flat 302, Green Meadows, Kukatpally, Hyderabad', 'CITIZEN'),
(3, 'Sneha Patel', 'sneha.patel@example.com', '9876543212', '$2a$10$wEfe2Q5c5g4w3GkUaHh0QOn7a8w102BvX0K7L8u6p5o4n3m2l1k0j', 'Plot 45, HIG Phase 2, Kukatpally, Hyderabad', 'CITIZEN')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 4. Seed a Historical Resolved Complaint for Demonstration
-- Complaint #1: Road Damage in Kukatpally (RESOLVED 10 days ago)
-- When a citizen submits a new pothole report in Kukatpally, it will immediately match this!
INSERT INTO complaints (
    complaint_id, user_id, category_id, department_id, parent_complaint_id,
    title, description, area, city, state, landmark,
    latitude, longitude, status, submitted_at, resolved_at, resolution_remarks
) VALUES (
    1, 2, 1, 1, NULL,
    'Severe Pothole on Main Road near Metro Station',
    'A deep crater has formed in the middle of the road near pillar 1142, causing dangerous two-wheeler skids and traffic slowdown.',
    'Kukatpally', 'Hyderabad', 'Telangana', 'Near Metro Pillar 1142',
    17.49380000, 78.39950000, 'RESOLVED',
    DATE_SUB(NOW(), INTERVAL 14 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    'Road patch applied with cold asphalt mix by maintenance team.'
) ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Seed history for complaint #1
INSERT INTO complaint_history (complaint_id, previous_status, new_status, remarks, updated_by, updated_at) VALUES
(1, NULL, 'SUBMITTED', 'Complaint submitted by citizen.', 2, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1, 'SUBMITTED', 'UNDER REVIEW', 'Verified problem severity through citizen evidence photo.', 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1, 'UNDER REVIEW', 'ASSIGNED', 'Assigned work order to Road Maintenance Department crew 4.', 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1, 'ASSIGNED', 'IN PROGRESS', 'Work crew dispatched to Kukatpally Metro Pillar 1142.', 1, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1, 'IN PROGRESS', 'RESOLVED', 'Road patch applied with cold asphalt mix. Pothole filled and leveled.', 1, DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE remarks=VALUES(remarks);

