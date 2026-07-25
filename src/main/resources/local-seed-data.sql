-- ============================================================
-- 시연용 초기 데이터
-- 관리자 계정: admin@mirocloset.com / admin1234!
-- 일반 회원 계정: user@mirocloset.com / user1234!
-- (둘 다 BCrypt 해시로 저장, 위 평문으로 로그인 가능)
-- 상품 이미지는 Unsplash의 실제 의류 사진(카테고리에 맞게 직접 선별) - 운영 전환 시 S3 URL로 교체
-- ============================================================

-- member_id 존재 여부가 아니라 email(고유키) 기준 UPSERT - 이렇게 해야 브랜드명 변경처럼
-- 시드 데이터 자체가 바뀌었을 때도 이미 시드된 로컬 DB에 재적용된다(예전엔 member_id만 보고
-- "이미 있으면 건너뛰기"라서 이메일 도메인을 바꿔도 기존 로컬 DB에는 반영되지 않았다).
INSERT INTO member (member_id, email, password, name, phone, address, role, status, created_at, updated_at)
VALUES (1, 'admin@mirocloset.com', '$2b$10$QJFflWyAEDIAQ579flaQRuelAQMmqMQIEcCH4r0DITMT9M9iEjVVq',
        '관리자', '010-0000-0000', '서울특별시 강남구 테헤란로 1', 'ROLE_ADMIN', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email), password = VALUES(password), name = VALUES(name);

INSERT INTO member (member_id, email, password, name, phone, address, role, status, created_at, updated_at)
VALUES (2, 'user@mirocloset.com', '$2b$10$bmSsBAg9tX/pezk6RSJ46.kf957n1GObXOUtUaIB1WtRqtQkOZ5Vu',
        '일반회원', '010-1234-5678', '서울특별시 마포구 양화로 1', 'ROLE_USER', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email), password = VALUES(password), name = VALUES(name);

-- 일반 회원가입(MemberService.register)은 가입과 동시에 카트를 만들지만, 시드로 직접 넣은 계정은
-- 그 경로를 거치지 않으므로 카트가 없다. 방어적으로 미리 만들어 둔다.
INSERT INTO cart (cart_id, member_id, created_at)
SELECT 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM cart WHERE member_id = 1);

INSERT INTO cart (cart_id, member_id, created_at)
SELECT 2, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM cart WHERE member_id = 2);

INSERT INTO category (category_id, name, display_order) VALUES
 (1, '아우터', 1),
 (2, '상의', 2),
 (3, '하의', 3),
 (4, '원피스', 4),
 (5, '액세서리', 5)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO product (product_id, category_id, name, price, description, thumbnail_url, stock_quantity, status, view_count, version, created_at, updated_at) VALUES
 (1, 1, '오버사이즈 울 코트', 189000, '따뜻한 울 혼방 오버사이즈 코트', 'https://images.unsplash.com/photo-1539533113208-f6df8cc8b543?w=600&h=750&fit=crop&q=80', 25, 'ON_SALE', 0, 0, NOW(), NOW()),
 (2, 1, '경량 바람막이 자켓', 79000, '사계절용 경량 바람막이', 'https://images.unsplash.com/photo-1611308725032-74f0a551d018?w=600&h=750&fit=crop&q=80', 40, 'ON_SALE', 0, 0, NOW(), NOW()),
 (3, 2, '베이직 코튼 티셔츠', 29000, '100% 코튼 기본 티셔츠', 'https://images.unsplash.com/photo-1620799139507-2a76f79a2f4d?w=600&h=750&fit=crop&q=80', 100, 'ON_SALE', 0, 0, NOW(), NOW()),
 (4, 2, '스트라이프 니트', 59000, '가을 스트라이프 니트웨어', 'https://images.unsplash.com/photo-1782226739532-73cbe6cfb204?w=600&h=750&fit=crop&q=80', 60, 'ON_SALE', 0, 0, NOW(), NOW()),
 (5, 3, '와이드 슬랙스', 69000, '허리 밴딩 와이드 슬랙스', 'https://images.unsplash.com/photo-1687825515654-23620796760c?w=600&h=750&fit=crop&q=80', 50, 'ON_SALE', 0, 0, NOW(), NOW()),
 (6, 3, '스트레이트 데님', 75000, '연청 스트레이트 데님팬츠', 'https://images.unsplash.com/photo-1714143136372-ddaf8b606da7?w=600&h=750&fit=crop&q=80', 45, 'ON_SALE', 0, 0, NOW(), NOW()),
 (7, 4, '린넨 셔츠 원피스', 99000, '여름용 린넨 셔츠 원피스', 'https://images.unsplash.com/photo-1747396206869-75ea57b325ce?w=600&h=750&fit=crop&q=80', 30, 'ON_SALE', 0, 0, NOW(), NOW()),
 (8, 4, '플리츠 미디 원피스', 89000, '우아한 플리츠 미디 원피스', 'https://images.unsplash.com/photo-1608078800752-6c0dba64d9d3?w=600&h=750&fit=crop&q=80', 20, 'ON_SALE', 0, 0, NOW(), NOW()),
 (9, 5, '레더 벨트', 39000, '천연가죽 클래식 벨트', 'https://images.unsplash.com/photo-1711443982852-b3df5c563448?w=600&h=750&fit=crop&q=80', 70, 'ON_SALE', 0, 0, NOW(), NOW()),
 (10, 5, '울 머플러', 45000, '겨울 필수 울 머플러', 'https://images.unsplash.com/photo-1485527691629-8e370684924c?w=600&h=750&fit=crop&q=80', 35, 'ON_SALE', 0, 0, NOW(), NOW()),
 (11, 2, '오버핏 후드티', 65000, '데일리 오버핏 후드 스웨트셔츠', 'https://images.unsplash.com/photo-1742392133846-a8b416e81661?w=600&h=750&fit=crop&q=80', 55, 'ON_SALE', 0, 0, NOW(), NOW()),
 (12, 1, '퀄팅 롱 패딩', 219000, '방한용 롱 패딩', 'https://images.unsplash.com/photo-1611025504703-8c143abe6996?w=600&h=750&fit=crop&q=80', 15, 'ON_SALE', 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), thumbnail_url = VALUES(thumbnail_url);

INSERT INTO product_image (product_id, image_url, sort_order) VALUES
 (1, 'https://images.unsplash.com/photo-1539533113208-f6df8cc8b543?w=600&h=750&fit=crop&q=80', 0),
 (1, 'https://images.unsplash.com/photo-1550872199-63f4382fe925?w=600&h=750&fit=crop&q=80', 1),
 (2, 'https://images.unsplash.com/photo-1611308725032-74f0a551d018?w=600&h=750&fit=crop&q=80', 0),
 (3, 'https://images.unsplash.com/photo-1620799139507-2a76f79a2f4d?w=600&h=750&fit=crop&q=80', 0),
 (4, 'https://images.unsplash.com/photo-1782226739532-73cbe6cfb204?w=600&h=750&fit=crop&q=80', 0),
 (5, 'https://images.unsplash.com/photo-1687825515654-23620796760c?w=600&h=750&fit=crop&q=80', 0),
 (6, 'https://images.unsplash.com/photo-1714143136372-ddaf8b606da7?w=600&h=750&fit=crop&q=80', 0),
 (7, 'https://images.unsplash.com/photo-1747396206869-75ea57b325ce?w=600&h=750&fit=crop&q=80', 0),
 (8, 'https://images.unsplash.com/photo-1608078800752-6c0dba64d9d3?w=600&h=750&fit=crop&q=80', 0),
 (9, 'https://images.unsplash.com/photo-1711443982852-b3df5c563448?w=600&h=750&fit=crop&q=80', 0),
 (10, 'https://images.unsplash.com/photo-1485527691629-8e370684924c?w=600&h=750&fit=crop&q=80', 0),
 (11, 'https://images.unsplash.com/photo-1742392133846-a8b416e81661?w=600&h=750&fit=crop&q=80', 0),
 (12, 'https://images.unsplash.com/photo-1611025504703-8c143abe6996?w=600&h=750&fit=crop&q=80', 0)
ON DUPLICATE KEY UPDATE image_url = VALUES(image_url);
