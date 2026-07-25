-- ============================================================
-- 미르옷장 - shopping_mall schema
-- 설계 원칙:
--   1) 회원/카테고리/상품/이미지/장바구니는 3NF 정규화 유지
--   2) product.thumbnail_url, orders.total_price, order_item.product_name/price 는
--      의도적 비정규화(읽기 성능 + 주문 이력의 불변성 확보) - 발표자료 4p 참고
-- ============================================================

CREATE TABLE IF NOT EXISTS member (
    member_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100)  NOT NULL,
    password        VARCHAR(255)  NOT NULL,
    name            VARCHAR(50)   NOT NULL,
    phone           VARCHAR(20),
    address         VARCHAR(255),
    role            VARCHAR(20)   NOT NULL DEFAULT 'ROLE_USER',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    provider        VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS category (
    category_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)   NOT NULL,
    display_order   INT           NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
    product_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT        NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    price           INT           NOT NULL,
    description     TEXT,
    thumbnail_url   VARCHAR(500),
    stock_quantity  INT           NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ON_SALE',
    view_count      INT           NOT NULL DEFAULT 0,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(category_id),
    INDEX idx_product_category_id (category_id),
    INDEX idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_image (
    product_image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT        NOT NULL,
    image_url       VARCHAR(500)  NOT NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE,
    CONSTRAINT uk_product_image_product_sort UNIQUE (product_id, sort_order),
    INDEX idx_product_image_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart (
    cart_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT        NOT NULL,
    created_at      DATETIME      NOT NULL,
    CONSTRAINT fk_cart_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    CONSTRAINT uk_cart_member UNIQUE (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart_item (
    cart_item_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id         BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    quantity        INT           NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL,
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(cart_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(product_id),
    CONSTRAINT uk_cart_item_cart_product UNIQUE (cart_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    order_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number      VARCHAR(30)   NOT NULL,
    member_id         BIGINT        NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ORDERED',
    total_price       INT           NOT NULL,
    receiver_name     VARCHAR(50)   NOT NULL,
    receiver_phone    VARCHAR(20)   NOT NULL,
    receiver_address  VARCHAR(255)  NOT NULL,
    carrier_code      VARCHAR(20)   NULL,
    tracking_number   VARCHAR(50)   NULL,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    INDEX idx_orders_member_id (member_id),
    INDEX idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    order_item_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    product_id      BIGINT        NULL,
    product_name    VARCHAR(100)  NOT NULL,
    price           INT           NOT NULL,
    quantity        INT           NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE SET NULL,
    INDEX idx_order_item_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_history (
    login_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT        NULL,
    email_attempted VARCHAR(100)  NOT NULL,
    login_at        DATETIME      NOT NULL,
    ip_address      VARCHAR(45),
    success         BOOLEAN       NOT NULL,
    CONSTRAINT fk_login_history_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE SET NULL,
    INDEX idx_login_history_member_id (member_id),
    INDEX idx_login_history_login_at (login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 마이그레이션 (이미 생성된 로컬/운영 DB에도 재기동 시 안전하게 재적용된다)
-- MySQL은 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 문법을 지원하지 않으므로(MariaDB 전용 문법),
-- information_schema로 존재 여부를 확인한 뒤 PREPARE/EXECUTE로 조건부 실행한다.
-- ============================================================

-- 구글 OAuth2 로그인 지원: 가입 경로 구분
SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND COLUMN_NAME = 'provider') = 0,
    "ALTER TABLE member ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'",
    'SELECT 1'
));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 택배사 배송조회 API 연동: 관리자가 배송중 전환 시 입력하는 운송장 정보
SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'carrier_code') = 0,
    "ALTER TABLE orders ADD COLUMN carrier_code VARCHAR(20) NULL, ADD COLUMN tracking_number VARCHAR(50) NULL",
    'SELECT 1'
));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
