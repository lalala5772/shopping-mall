-- ============================================================
-- 운영 DB 기능 점검용 테스트 주문 데이터.
-- merongai66@gmail.com(member_id=1) 계정에 다양한 상태(ORDERED/SHIPPING/DELIVERED/CANCELLED)의
-- 주문을 넣어서 마이페이지 주문내역, 관리자 주문관리, 관리자 대시보드 집계가 실제로
-- 잘 작동하는지 화면에서 바로 확인할 수 있게 한다.
--
-- product.stock_quantity는 실제 주문 시 차감되는 것과 동일하게 여기서도 맞춰서 뺐다
-- (CANCELLED 건은 취소 시 복원되는 것과 같은 효과로 순증감 0으로 남겨둔다).
--
-- 실행 예 (utf8mb4 필수 - 안 붙이면 receiver_address 등 한글이 깨진다):
--   docker compose exec -T mysql mysql --default-character-set=utf8mb4 -u root -p"$DB_ROOT_PASSWORD" shopping_mall < scripts/seed-prod-test-orders.sql
-- ============================================================

-- 주문 A: DELIVERED (10일 전, 배송 완료까지 전 과정 확인용)
INSERT INTO orders (order_id, order_number, member_id, status, total_price, receiver_name, receiver_phone, receiver_address, carrier_code, tracking_number, created_at, updated_at)
VALUES (101, 'ORD-TEST0001-A1B2C3D4', 1, 'DELIVERED', 247000, '이마르', '010-1234-5678', '서울특별시 강남구 테헤란로 123, 101동 1001호',
        '04', '123456789012', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), total_price = VALUES(total_price);

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, price, quantity)
VALUES
 (101, 101, 1, '오버사이즈 울 코트', 189000, 1),
 (102, 101, 3, '베이직 코튼 티셔츠', 29000, 2)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), price = VALUES(price), quantity = VALUES(quantity);

-- 주문 B: SHIPPING (2일 전, 배송조회 API 연동 확인용 - 실제 스마트택배 테스트 운송장 번호는 아님)
INSERT INTO orders (order_id, order_number, member_id, status, total_price, receiver_name, receiver_phone, receiver_address, carrier_code, tracking_number, created_at, updated_at)
VALUES (102, 'ORD-TEST0002-B2C3D4E5', 1, 'SHIPPING', 69000, '이마르', '010-1234-5678', '서울특별시 강남구 테헤란로 123, 101동 1001호',
        '05', '987654321098', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), total_price = VALUES(total_price);

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, price, quantity)
VALUES (103, 102, 5, '와이드 슬랙스', 69000, 1)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), price = VALUES(price), quantity = VALUES(quantity);

-- 주문 C: ORDERED (방금 주문, 관리자가 배송정보 입력하는 흐름 확인용 - 운송장 미배정 상태)
INSERT INTO orders (order_id, order_number, member_id, status, total_price, receiver_name, receiver_phone, receiver_address, carrier_code, tracking_number, created_at, updated_at)
VALUES (103, 'ORD-TEST0003-C3D4E5F6', 1, 'ORDERED', 84000, '이마르', '010-1234-5678', '서울특별시 강남구 테헤란로 123, 101동 1001호',
        NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), total_price = VALUES(total_price);

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, price, quantity)
VALUES
 (104, 103, 9, '레더 벨트', 39000, 1),
 (105, 103, 10, '울 머플러', 45000, 1)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), price = VALUES(price), quantity = VALUES(quantity);

-- 주문 D: CANCELLED (7일 전, 취소 시 재고 복원 로직이 이미 반영된 상태로 넣는다)
INSERT INTO orders (order_id, order_number, member_id, status, total_price, receiver_name, receiver_phone, receiver_address, carrier_code, tracking_number, created_at, updated_at)
VALUES (104, 'ORD-TEST0004-D4E5F6A7', 1, 'CANCELLED', 219000, '이마르', '010-1234-5678', '서울특별시 강남구 테헤란로 123, 101동 1001호',
        NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), total_price = VALUES(total_price);

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, price, quantity)
VALUES (106, 104, 12, '퀄팅 롱 패딩', 219000, 1)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), price = VALUES(price), quantity = VALUES(quantity);

-- 재고 차감: 실제로 주문 API를 거쳐 들어온 것과 동일한 최종 상태를 맞춘다.
-- (CANCELLED 건인 product 12는 취소로 복원되는 게 정상이므로 건드리지 않는다)
UPDATE product SET stock_quantity = 24 WHERE product_id = 1 AND stock_quantity = 25;
UPDATE product SET stock_quantity = 98 WHERE product_id = 3 AND stock_quantity = 100;
UPDATE product SET stock_quantity = 49 WHERE product_id = 5 AND stock_quantity = 50;
UPDATE product SET stock_quantity = 69 WHERE product_id = 9 AND stock_quantity = 70;
UPDATE product SET stock_quantity = 34 WHERE product_id = 10 AND stock_quantity = 35;
