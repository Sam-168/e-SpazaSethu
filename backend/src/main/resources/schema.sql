-- ============================================================
-- e-SpazaSethu — MySQL Database Schema
-- Version: 1.2 (snake_case columns, VARCHAR(36) ids)
-- Description: Full schema for POS & Inventory Management System
-- Run this on a fresh espaza_db database
-- Column names are snake_case to match Spring Boot's default
-- Hibernate naming strategy (categoryId -> category_id).
-- ============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS espaza_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE espaza_db;

-- ============================================================
-- TABLE ORDER MATTERS — FK dependencies created first
-- 1. category
-- 2. user
-- 3. product        (FK → category)
-- 4. sale           (FK → user)
-- 5. sale_item      (FK → sale, product)
-- 6. stock_movement (FK → product, user)
-- 7. stocktake      (FK → user)
-- 8. stocktake_item (FK → stocktake, product)
-- ============================================================


-- ------------------------------------------------------------
-- 1. CATEGORY
-- ------------------------------------------------------------
CREATE TABLE category (
                          category_id   VARCHAR(36)        NOT NULL,
                          name          VARCHAR(100)    NOT NULL,
                          description   TEXT,
                          created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT pk_category     PRIMARY KEY (category_id),
                          CONSTRAINT uq_category_name UNIQUE (name)
);


-- ------------------------------------------------------------
-- 2. USER
-- ------------------------------------------------------------
CREATE TABLE user (
                      user_id        VARCHAR(36)        NOT NULL,
                      username       VARCHAR(50)     NOT NULL,
                      password_hash  VARCHAR(255)    NOT NULL,
                      role           ENUM('ADMIN', 'CASHIER') NOT NULL DEFAULT 'CASHIER',
                      is_active      TINYINT(1)      NOT NULL DEFAULT 1,
                      last_login     DATETIME,
                      created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT pk_user       PRIMARY KEY (user_id),
                      CONSTRAINT uq_username   UNIQUE (username)
);


-- ------------------------------------------------------------
-- 3. PRODUCT
-- ------------------------------------------------------------
CREATE TABLE product (
                         product_id          VARCHAR(36)        NOT NULL,
                         category_id         VARCHAR(36),
                         name                VARCHAR(200)    NOT NULL,
                         barcode             VARCHAR(100),
                         selling_price       DECIMAL(10, 2)  NOT NULL,
                         cost_price          DECIMAL(10, 2),
                         stock_quantity      INT             NOT NULL DEFAULT 0,
                         low_stock_threshold INT,
                         description         TEXT,
                         is_active           TINYINT(1)      NOT NULL DEFAULT 1,
                         created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT pk_product           PRIMARY KEY (product_id),
                         CONSTRAINT uq_product_barcode   UNIQUE (barcode),
                         CONSTRAINT fk_product_category  FOREIGN KEY (category_id) REFERENCES category (category_id) ON DELETE SET NULL,
                         CONSTRAINT chk_selling_price    CHECK (selling_price >= 0),
                         CONSTRAINT chk_cost_price       CHECK (cost_price IS NULL OR cost_price >= 0),
                         CONSTRAINT chk_stock_qty        CHECK (stock_quantity >= 0),
                         CONSTRAINT chk_low_stock        CHECK (low_stock_threshold IS NULL OR low_stock_threshold >= 0)
);

-- Index for product search by name
CREATE INDEX idx_product_name     ON product (name);
-- Index for barcode lookup (POS scanning)
CREATE INDEX idx_product_barcode  ON product (barcode);
-- Index for filtering active products
CREATE INDEX idx_product_active   ON product (is_active);
-- Index for low-stock queries
CREATE INDEX idx_product_low_stock ON product (stock_quantity, low_stock_threshold);


-- ------------------------------------------------------------
-- 4. SALE
-- ------------------------------------------------------------
CREATE TABLE sale (
                      sale_id           VARCHAR(36)        NOT NULL,
                      user_id           VARCHAR(36)        NOT NULL,
                      sale_date_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      total_amount      DECIMAL(10, 2)  NOT NULL,
                      payment_method    ENUM('CASH', 'CARD', 'MOBILE_PAYMENT') NOT NULL,
                      status            ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'COMPLETED',
                      notes             TEXT,
                      created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT pk_sale        PRIMARY KEY (sale_id),
                      CONSTRAINT fk_sale_user   FOREIGN KEY (user_id) REFERENCES user (user_id),
                      CONSTRAINT chk_total      CHECK (total_amount >= 0)
);

-- Index for date-range queries (sales history, dashboard)
CREATE INDEX idx_sale_datetime  ON sale (sale_date_time);
-- Index for filtering by user
CREATE INDEX idx_sale_user      ON sale (user_id);
-- Composite index for dashboard queries (status + date)
CREATE INDEX idx_sale_status_date ON sale (status, sale_date_time);


-- ------------------------------------------------------------
-- 5. SALE_ITEM
-- ------------------------------------------------------------
CREATE TABLE sale_item (
                           sale_item_id  VARCHAR(36)        NOT NULL,
                           sale_id       VARCHAR(36)        NOT NULL,
                           product_id    VARCHAR(36)        NOT NULL,
                           quantity      INT             NOT NULL,
                           unit_price    DECIMAL(10, 2)  NOT NULL,
                           subtotal      DECIMAL(10, 2)  NOT NULL,

                           CONSTRAINT pk_sale_item         PRIMARY KEY (sale_item_id),
                           CONSTRAINT fk_sale_item_sale    FOREIGN KEY (sale_id)    REFERENCES sale (sale_id)       ON DELETE CASCADE,
                           CONSTRAINT fk_sale_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
                           CONSTRAINT chk_quantity         CHECK (quantity > 0),
                           CONSTRAINT chk_unit_price       CHECK (unit_price >= 0),
                           CONSTRAINT chk_subtotal         CHECK (subtotal >= 0)
);

-- Index for loading all items in a sale
CREATE INDEX idx_sale_item_sale       ON sale_item (sale_id);
-- Index for top-products report (group by product across sales)
CREATE INDEX idx_sale_item_product    ON sale_item (product_id);


-- ------------------------------------------------------------
-- 6. STOCK_MOVEMENT
-- ------------------------------------------------------------
CREATE TABLE stock_movement (
                                movement_id      VARCHAR(36)        NOT NULL,
                                product_id       VARCHAR(36)        NOT NULL,
                                created_by       VARCHAR(36)        NOT NULL,
                                quantity_change  INT             NOT NULL,
                                movement_type    ENUM('SALE', 'ADJUSTMENT', 'STOCKTAKE_CORRECTION') NOT NULL,
                                reference_id     VARCHAR(36),
                                notes            TEXT,
                                created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT pk_stock_movement          PRIMARY KEY (movement_id),
                                CONSTRAINT fk_movement_product        FOREIGN KEY (product_id)  REFERENCES product (product_id),
                                CONSTRAINT fk_movement_user           FOREIGN KEY (created_by)  REFERENCES user (user_id)
    -- reference_id is intentionally not a strict FK — it can point to a sale or stocktake
    -- depending on movement_type. Application layer resolves the reference.
);

-- Index for audit log queries (filter by product)
CREATE INDEX idx_movement_product   ON stock_movement (product_id);
-- Index for date-range filtering on the audit log
CREATE INDEX idx_movement_date      ON stock_movement (created_at);
-- Index for filtering by movement type
CREATE INDEX idx_movement_type      ON stock_movement (movement_type);
-- Composite index for product history queries
CREATE INDEX idx_movement_product_date ON stock_movement (product_id, created_at);


-- ------------------------------------------------------------
-- 7. STOCKTAKE
-- ------------------------------------------------------------
CREATE TABLE stocktake (
                           stocktake_id  VARCHAR(36)        NOT NULL,
                           conducted_by  VARCHAR(36)        NOT NULL,
                           started_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           completed_at  DATETIME,
                           status        ENUM('IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'IN_PROGRESS',
                           notes         TEXT,

                           CONSTRAINT pk_stocktake       PRIMARY KEY (stocktake_id),
                           CONSTRAINT fk_stocktake_user  FOREIGN KEY (conducted_by) REFERENCES user (user_id)
);

-- Index for finding in-progress stocktakes per user
CREATE INDEX idx_stocktake_user_status ON stocktake (conducted_by, status);
-- Index for listing stocktakes by date
CREATE INDEX idx_stocktake_started     ON stocktake (started_at);


-- ------------------------------------------------------------
-- 8. STOCKTAKE_ITEM
-- ------------------------------------------------------------
CREATE TABLE stocktake_item (
                                stocktake_item_id   VARCHAR(36)    NOT NULL,
                                stocktake_id        VARCHAR(36)    NOT NULL,
                                product_id          VARCHAR(36)    NOT NULL,
                                system_quantity     INT         NOT NULL,
                                counted_quantity    INT,
                                discrepancy         INT,
                                adjusted            TINYINT(1)  NOT NULL DEFAULT 0,

                                CONSTRAINT pk_stocktake_item            PRIMARY KEY (stocktake_item_id),
                                CONSTRAINT fk_stocktake_item_stocktake  FOREIGN KEY (stocktake_id) REFERENCES stocktake (stocktake_id) ON DELETE CASCADE,
                                CONSTRAINT fk_stocktake_item_product    FOREIGN KEY (product_id)   REFERENCES product (product_id),
    -- Each product appears only once per stocktake
                                CONSTRAINT uq_stocktake_product         UNIQUE (stocktake_id, product_id)
);

-- Index for loading all items in a stocktake
CREATE INDEX idx_stocktake_item_stocktake ON stocktake_item (stocktake_id);
-- Index for discrepancy queries (filter out nulls and zeros)
CREATE INDEX idx_stocktake_item_discrep   ON stocktake_item (stocktake_id, discrepancy);


-- ============================================================
-- SEED DATA — Default admin user for local development
-- Password is BCrypt hash of "Admin@123"
-- CHANGE THIS BEFORE DEPLOYING TO ANY SHARED ENVIRONMENT
-- ============================================================

CREATE TABLE IF NOT EXISTS store_settings (
    settings_id INT NOT NULL PRIMARY KEY,
    shop_name VARCHAR(120) NOT NULL,
    currency_code VARCHAR(3) NOT NULL
);

INSERT INTO store_settings (settings_id, shop_name, currency_code)
SELECT 1, 'e-SpazaSethu', 'ZAR'
WHERE NOT EXISTS (SELECT 1 FROM store_settings WHERE settings_id = 1);

CREATE TABLE IF NOT EXISTS sale_item_cost_snapshot (
    sale_item_id VARCHAR(36) NOT NULL PRIMARY KEY,
    cost_price DECIMAL(10, 2) NOT NULL
);


INSERT INTO user (user_id, username, password_hash, role, is_active, created_at)
VALUES (
           'a0000000-0000-0000-0000-000000000001',
           'admin',
           '$2a$12$kVmhpJTzFYoLjbpbVQZFCOjh6FHmVHQv3JZMQpDRPrDnOWnCcg3.6',
           'ADMIN',
           1,
           NOW()
       );

-- ============================================================
-- SEED DATA — Sample categories (optional, can be deleted)
-- ============================================================

INSERT INTO category (category_id, name, description, created_at) VALUES
                                                                      ('c0000000-0000-0000-0000-000000000001', 'Beverages',  'Cold drinks, juices, water',        NOW()),
                                                                      ('c0000000-0000-0000-0000-000000000002', 'Snacks',     'Chips, biscuits, sweets, chocolates', NOW()),
                                                                      ('c0000000-0000-0000-0000-000000000003', 'Airtime',    'Prepaid airtime and data vouchers',   NOW()),
                                                                      ('c0000000-0000-0000-0000-000000000004', 'Household',  'Cleaning products, toiletries',       NOW()),
                                                                      ('c0000000-0000-0000-0000-000000000005', 'Bread & Dairy', 'Bread, milk, eggs, butter',       NOW());

-- ============================================================
-- SEED DATA — Sample products (optional, useful for testing)
-- ============================================================

INSERT INTO product (product_id, category_id, name, barcode, selling_price, cost_price, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Coca-Cola 500ml',    '6001056000006', 15.00, 10.00, 48, 12, 1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'Fanta Orange 500ml', '6001056000013', 15.00, 10.00, 36, 12, 1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'Water 500ml',        '6009705522009', 8.00,  5.00,  60, 20, 1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', 'Lays Chips 120g',    '6001480005008', 22.00, 15.00, 24, 6,  1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'Oreos 154g',         '7622210044860', 28.00, 19.00, 18, 6,  1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005', 'Albany Bread 700g',  '6001499000055', 19.99, 14.00, 10, 5,  1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005', 'Clover Milk 1L',     '6001209000020', 24.99, 18.00, 15, 5,  1, NOW(), NOW()),
                                                                                                                                                                    ('p0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003', 'Vodacom R10 Airtime', NULL,           10.00, 10.00, 50, 10, 1, NOW(), NOW());

-- ============================================================
-- END OF SCHEMA
-- ============================================================
