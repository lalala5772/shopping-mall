-- ============================================================
-- 운영(prod) DB용 상품 데이터 시딩 스크립트.
-- local-seed-data.sql과 달리 계정(member)/카트는 절대 포함하지 않는다 - prod에
-- 알려진 비밀번호의 데모 계정이 생기는 걸 막으려는 게 애초에 이 분리의 목적이었다.
-- Spring Boot의 sql.init 자동 실행 대상이 아니다(classpath 밖, 파일명도 data.sql이 아님) -
-- 최초 배포 직후 딱 한 번, 운영자가 직접 mysql 클라이언트로 실행한다.
--
-- 실행 예:
--   docker compose exec -T mysql mysql -u root -p"$DB_ROOT_PASSWORD" shopping_mall < scripts/seed-prod-products.sql
-- ============================================================

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
