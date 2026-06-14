-- Backup of t_menu before menu regrouping
-- Restore with: delete from t_menu; then run inserts below

DELETE FROM t_menu;
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (2, 'ข้อมูลพื้นฐาน', 'Y', NULL, NULL, 'bi bi-stack', 2);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (3, 'ข้อมูลศิลปิน', NULL, 2, '/artist', NULL, 1);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (4, 'ข้อมูลประเภทสินค้า', NULL, 2, '/type', NULL, 2);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (5, 'ข้อมูลเว็บไซต์', NULL, 2, '/website', NULL, 3);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (6, 'ข้อมูลสินค้า', 'N', NULL, '/product', 'bi bi-book-half', 4);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (7, 'บันทึกออร์เดอร์', 'N', NULL, '/order', 'bi bi-basket', 5);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (8, 'อัปเดทสถานะ', 'N', NULL, '/change', 'bi bi-check-circle-fill', 8);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (9, 'ข้อมูลการกดของ', 'N', NULL, '/check_status_2', 'bi bi-clipboard-check', 9);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (10, 'ข้อมูล LOT', 'N', NULL, '/lot', 'bi bi-tags-fill', 10);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (11, 'ข้อมูลค่าชิปปิ้ง', 'N', NULL, '/cost/shipping', 'bi bi-truck', 11);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (12, 'รายการสั่งซื้อ', 'N', NULL, '/orders', 'bi bi-receipt', 6);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (13, 'บันทึกรับเงินมัดจำ', 'N', NULL, '/deposit-balance', 'bi bi-cash-stack', 7);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (14, 'ปฏิทินกำหนดการ', 'N', NULL, '/schedule-calendar', 'bi bi-calendar-event', 1);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (15, 'รายงาน', 'Y', NULL, NULL, 'bi bi-file-earmark-bar-graph', 13);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (16, 'บันทึกค่าใช้จ่าย', 'N', NULL, '/cost/expense', 'bi bi-credit-card', 12);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (17, 'คำนวณราคาขาย', 'N', NULL, '/pricing-calculator', 'bi bi-calculator', 3);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (19, 'PT00 รายงานรวม', NULL, 15, '/reports/PT00', NULL, 1);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (20, 'PT01 รายงานบัญชีรายรับ รายจ่าย', NULL, 15, '/reports/PT01', NULL, 2);
INSERT INTO t_menu (id_menu, m_name, m_parent, m_id_menu, m_url, m_icon, m_order) VALUES (21, 'PT02 รายงานกำไรขาดทุน', NULL, 15, '/reports/PT02', NULL, 3);
