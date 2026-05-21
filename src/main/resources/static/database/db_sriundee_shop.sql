/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80042
 Source Host           : localhost:3306
 Source Schema         : db_sriundee_shop

 Target Server Type    : MySQL
 Target Server Version : 80042
 File Encoding         : 65001

 Date: 21/05/2026 14:49:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_artist
-- ----------------------------
DROP TABLE IF EXISTS `t_artist`;
CREATE TABLE `t_artist`  (
  `ID_art` int(0) NOT NULL AUTO_INCREMENT,
  `a_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_group` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `a_logo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `a_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_art`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_cost
-- ----------------------------
DROP TABLE IF EXISTS `t_cost`;
CREATE TABLE `t_cost`  (
  `ID_cost` int(0) NOT NULL AUTO_INCREMENT,
  `c_create_date` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_type_cost` int(0) DEFAULT NULL,
  `c_price` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_note` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_cost`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_cost_detail
-- ----------------------------
DROP TABLE IF EXISTS `t_cost_detail`;
CREATE TABLE `t_cost_detail`  (
  `ID_cost_detail` int(0) NOT NULL AUTO_INCREMENT,
  `ID_cost` int(0) DEFAULT NULL,
  `ID_order_detail` int(0) DEFAULT NULL,
  PRIMARY KEY (`ID_cost_detail`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_cover
-- ----------------------------
DROP TABLE IF EXISTS `t_cover`;
CREATE TABLE `t_cover`  (
  `ID_cover` int(0) NOT NULL AUTO_INCREMENT,
  `ID_pro` int(0) DEFAULT NULL,
  `ID_web` int(0) DEFAULT NULL,
  `ID_ver` int(0) DEFAULT NULL,
  `c_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_price_total` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_price_pledge` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_price_balance` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_cover`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_group
-- ----------------------------
DROP TABLE IF EXISTS `t_group`;
CREATE TABLE `t_group`  (
  `ID_group` int(0) NOT NULL,
  `g_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `g_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_group
-- ----------------------------
INSERT INTO `t_group` VALUES (1, 'SM', 'A');
INSERT INTO `t_group` VALUES (2, 'YG', 'A');
INSERT INTO `t_group` VALUES (3, 'JYP', 'A');
INSERT INTO `t_group` VALUES (4, 'HYPE', 'A');
INSERT INTO `t_group` VALUES (5, 'อื่น ๆ', 'A');

-- ----------------------------
-- Table structure for t_income
-- ----------------------------
DROP TABLE IF EXISTS `t_income`;
CREATE TABLE `t_income`  (
  `ID_income` int(0) NOT NULL AUTO_INCREMENT,
  `c_create_date` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_customer_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_type_income` int(0) DEFAULT NULL,
  `c_price` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_note` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `c_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_order` int(0) DEFAULT NULL,
  `ID_sale` int(0) DEFAULT NULL,
  PRIMARY KEY (`ID_income`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_lot
-- ----------------------------
DROP TABLE IF EXISTS `t_lot`;
CREATE TABLE `t_lot`  (
  `ID_lot` int(0) NOT NULL AUTO_INCREMENT,
  `l_lot_number` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `l_create_date` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `l_start_date` date DEFAULT NULL,
  `l_end_date` date DEFAULT NULL,
  `l_arrive_date` date DEFAULT NULL,
  `l_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_lot`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_lot_detail
-- ----------------------------
DROP TABLE IF EXISTS `t_lot_detail`;
CREATE TABLE `t_lot_detail`  (
  `ID_lot_detail` int(0) NOT NULL AUTO_INCREMENT,
  `ID_lot` int(0) DEFAULT NULL,
  `ID_order_detail` int(0) DEFAULT NULL,
  PRIMARY KEY (`ID_lot_detail`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu`  (
  `ID_menu` int(0) NOT NULL AUTO_INCREMENT,
  `m_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `m_parent` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `m_ID_menu` int(0) DEFAULT NULL,
  `m_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `m_icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_menu`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES (2, 'ข้อมูลพื้นฐาน', 'Y', NULL, NULL, 'bi bi-stack');
INSERT INTO `t_menu` VALUES (3, 'ข้อมูลศิลปิน', NULL, 2, '/artist', NULL);
INSERT INTO `t_menu` VALUES (4, 'ข้อมูลประเภทสินค้า', NULL, 2, '/type', NULL);
INSERT INTO `t_menu` VALUES (5, 'ข้อมูลเว็บไซต์', NULL, 2, '/website', NULL);
INSERT INTO `t_menu` VALUES (6, 'ข้อมูลสินค้า', 'N', NULL, '/product', 'bi bi-book-half');
INSERT INTO `t_menu` VALUES (7, 'บันทึกออร์เดอร์', 'N', NULL, '/order', 'bi bi-basket');
INSERT INTO `t_menu` VALUES (8, 'อัปเดทสถานะ', 'N', NULL, '/change', 'bi bi-check-circle-fill');
INSERT INTO `t_menu` VALUES (9, 'ข้อมูลการกดของ', 'N', NULL, '/check_status_2', 'bi bi-clipboard-check');
INSERT INTO `t_menu` VALUES (10, 'ข้อมูล LOT', 'N', NULL, '/lot', 'bi bi-tags-fill');
INSERT INTO `t_menu` VALUES (11, 'ข้อมูลค่าชิปปิ้ง', 'N', NULL, '/cost/shipping', 'bi bi-truck');
INSERT INTO `t_menu` VALUES (12, 'รายการสั่งซื้อ', 'N', NULL, '/orders', 'bi bi-receipt');
INSERT INTO `t_menu` VALUES (13, 'บันทึกรับเงินมัดจำ', 'N', NULL, '/deposit-balance', 'bi bi-cash-stack');
INSERT INTO `t_menu` VALUES (14, 'ปฏิทินกำหนดการ', 'N', NULL, '/schedule-calendar', 'bi bi-calendar-event');
INSERT INTO `t_menu` VALUES (15, 'รายงาน', 'N', NULL, '/reports', 'bi bi-file-earmark-bar-graph');

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `ID_order` int(0) NOT NULL AUTO_INCREMENT,
  `o_order_date` date DEFAULT NULL,
  `o_order_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `o_customer_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_pay_method` int(0) DEFAULT NULL,
  `ID_pay_type` int(0) DEFAULT NULL,
  `o_last_pay_date` date DEFAULT NULL,
  `o_send_cost` double(255, 0) DEFAULT NULL,
  `o_discount` double(255, 0) DEFAULT NULL,
  `o_price_total` double(255, 0) DEFAULT NULL,
  `o_price_pledge` double(255, 0) DEFAULT NULL,
  `o_price_balance` double(255, 0) DEFAULT NULL,
  `o_net` double(255, 0) DEFAULT NULL,
  `o_remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_order`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order_detail
-- ----------------------------
DROP TABLE IF EXISTS `t_order_detail`;
CREATE TABLE `t_order_detail`  (
  `ID_order_detail` int(0) NOT NULL AUTO_INCREMENT,
  `ID_order` int(0) DEFAULT NULL,
  `ID_cover` int(0) DEFAULT NULL,
  `od_qty` int(0) DEFAULT NULL,
  `od_price_total` double(255, 0) DEFAULT NULL,
  `od_price_pledge` double(255, 0) DEFAULT NULL,
  `od_price_balance` double(255, 0) DEFAULT NULL,
  `ID_order_status` int(0) DEFAULT NULL,
  PRIMARY KEY (`ID_order_detail`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order_status
-- ----------------------------
DROP TABLE IF EXISTS `t_order_status`;
CREATE TABLE `t_order_status`  (
  `ID_order_status` int(0) NOT NULL,
  `os_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `os_color` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_order_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_order_status
-- ----------------------------
INSERT INTO `t_order_status` VALUES (1, 'รอกดของ', 'warning');
INSERT INTO `t_order_status` VALUES (2, 'กดของแล้ว รอเข้าโกดัง', 'info');
INSERT INTO `t_order_status` VALUES (3, 'รอของถึงร้าน', 'orange');
INSERT INTO `t_order_status` VALUES (4, 'ถึงร้านแล้วรอส่ง', 'danger');
INSERT INTO `t_order_status` VALUES (5, 'ส่งของสำเร็จ', 'success');

-- ----------------------------
-- Table structure for t_payment_method
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_method`;
CREATE TABLE `t_payment_method`  (
  `ID_pay_method` int(0) DEFAULT NULL,
  `pm_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `pm_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_payment_method
-- ----------------------------
INSERT INTO `t_payment_method` VALUES (1, 'จ่ายเต็ม', 'A');
INSERT INTO `t_payment_method` VALUES (2, 'มัดจำ', 'A');
INSERT INTO `t_payment_method` VALUES (3, 'ชำระมัดจำแล้ว', 'A');

-- ----------------------------
-- Table structure for t_payment_type
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_type`;
CREATE TABLE `t_payment_type`  (
  `ID_pay_type` int(0) NOT NULL,
  `pt_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_pay_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_payment_type
-- ----------------------------
INSERT INTO `t_payment_type` VALUES (1, 'ของถึงไทย');
INSERT INTO `t_payment_type` VALUES (2, 'กำหนดวัน');

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `ID_product` int(0) NOT NULL AUTO_INCREMENT,
  `p_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_type` int(0) DEFAULT NULL,
  `ID_art` int(0) DEFAULT NULL,
  `p_end_date` date DEFAULT NULL,
  `p_send_date` date DEFAULT NULL,
  `ID_pro_status` int(0) DEFAULT NULL,
  `p_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `p_pic` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_product`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_product_status
-- ----------------------------
DROP TABLE IF EXISTS `t_product_status`;
CREATE TABLE `t_product_status`  (
  `ID_pro_status` int(0) NOT NULL,
  `ps_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_pro_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_product_status
-- ----------------------------
INSERT INTO `t_product_status` VALUES (1, 'เปิดพรี');
INSERT INTO `t_product_status` VALUES (2, 'ปิดพรี');

-- ----------------------------
-- Table structure for t_product_web
-- ----------------------------
DROP TABLE IF EXISTS `t_product_web`;
CREATE TABLE `t_product_web`  (
  `ID_pro_web` int(0) NOT NULL AUTO_INCREMENT,
  `ID_pro` int(0) DEFAULT NULL,
  `ID_web` int(0) DEFAULT NULL,
  PRIMARY KEY (`ID_pro_web`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_sale
-- ----------------------------
DROP TABLE IF EXISTS `t_sale`;
CREATE TABLE `t_sale`  (
  `ID_sale` int(0) NOT NULL AUTO_INCREMENT,
  `s_customer_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_product_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_type` int(0) DEFAULT NULL,
  `ID_art` int(0) DEFAULT NULL,
  `s_price` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_num` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_shiping` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_net` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_note` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ID_pre_status` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_pending` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_send_date` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_sale`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_settings
-- ----------------------------
DROP TABLE IF EXISTS `t_settings`;
CREATE TABLE `t_settings`  (
  `ID_setting` int(0) NOT NULL AUTO_INCREMENT,
  `s_key` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `s_value` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_setting`) USING BTREE,
  UNIQUE INDEX `idx_t_settings_key`(`s_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_settings
-- ----------------------------
INSERT INTO `t_settings` VALUES (1, 'theme_mode', 'dark');
INSERT INTO `t_settings` VALUES (2, 'schedule_show_completed', 'true');
INSERT INTO `t_settings` VALUES (3, 'dashboard_chart_series', 'amount,receivedPaid,pledgePaid,pressCost,shippingCost');
INSERT INTO `t_settings` VALUES (4, 'dashboard_chart_granularity', 'day');

-- ----------------------------
-- Table structure for t_type
-- ----------------------------
DROP TABLE IF EXISTS `t_type`;
CREATE TABLE `t_type`  (
  `ID_type` int(0) NOT NULL AUTO_INCREMENT,
  `t_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `t_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_type_cost
-- ----------------------------
DROP TABLE IF EXISTS `t_type_cost`;
CREATE TABLE `t_type_cost`  (
  `ID_type_cost` int(0) NOT NULL,
  `tc_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_type_cost`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_type_cost
-- ----------------------------
INSERT INTO `t_type_cost` VALUES (1, 'ค่ากดของ');
INSERT INTO `t_type_cost` VALUES (2, 'ค่าชิปปิ้ง');
INSERT INTO `t_type_cost` VALUES (3, 'ค่าสั่งกล่อง');
INSERT INTO `t_type_cost` VALUES (4, 'ค่าสั่งทำของแถม');
INSERT INTO `t_type_cost` VALUES (5, 'ค่าส่งไปรษณีย์');
INSERT INTO `t_type_cost` VALUES (99, 'อื่น ๆ');

-- ----------------------------
-- Table structure for t_type_income
-- ----------------------------
DROP TABLE IF EXISTS `t_type_income`;
CREATE TABLE `t_type_income`  (
  `ID_type_income` int(0) NOT NULL,
  `ti_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_type_income`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_type_income
-- ----------------------------
INSERT INTO `t_type_income` VALUES (1, 'ค่าของ ( จ่ายเต็ม )');
INSERT INTO `t_type_income` VALUES (2, 'ค่าของ ( มัดจำ )');
INSERT INTO `t_type_income` VALUES (3, 'ยอดมัดจำที่เหลือ');
INSERT INTO `t_type_income` VALUES (4, 'ของพร้อมส่ง');

-- ----------------------------
-- Table structure for t_version
-- ----------------------------
DROP TABLE IF EXISTS `t_version`;
CREATE TABLE `t_version`  (
  `ID_ver` int(0) NOT NULL AUTO_INCREMENT,
  `ID_pro` int(0) DEFAULT NULL,
  `v_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `v_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_ver`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_website
-- ----------------------------
DROP TABLE IF EXISTS `t_website`;
CREATE TABLE `t_website`  (
  `ID_web` int(0) NOT NULL AUTO_INCREMENT,
  `w_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `w_delete` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_web`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- View structure for q_artist
-- ----------------------------
DROP VIEW IF EXISTS `q_artist`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_artist` AS select `t_artist`.`ID_art` AS `ID_art`,`t_artist`.`a_name` AS `a_name`,`t_artist`.`ID_group` AS `ID_group`,`t_group`.`g_name` AS `g_name`,`t_artist`.`a_logo` AS `a_logo`,`t_artist`.`a_delete` AS `a_delete` from (`t_artist` join `t_group` on((`t_artist`.`ID_group` = `t_group`.`ID_group`)));

-- ----------------------------
-- View structure for q_cost
-- ----------------------------
DROP VIEW IF EXISTS `q_cost`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_cost` AS select `t_cost`.`ID_cost` AS `ID_cost`,`t_cost`.`c_create_date` AS `c_create_date`,`t_cost`.`ID_type_cost` AS `ID_type_cost`,`t_type_cost`.`tc_name` AS `tc_name`,`t_cost`.`c_price` AS `c_price`,`t_cost`.`c_note` AS `c_note`,`t_cost`.`c_delete` AS `c_delete` from (`t_cost` join `t_type_cost` on((`t_cost`.`ID_type_cost` = `t_type_cost`.`ID_type_cost`)));

-- ----------------------------
-- View structure for q_cost_detail
-- ----------------------------
DROP VIEW IF EXISTS `q_cost_detail`;
CREATE ALGORITHM = UNDEFINED DEFINER = `sriundee_shop`@`localhost` SQL SECURITY DEFINER VIEW `q_cost_detail` AS select `t_cost_detail`.`ID_cost_detail` AS `ID_cost_detail`,`q_cost`.`ID_cost` AS `ID_cost`,`q_cost`.`c_create_date` AS `c_create_date`,`q_cost`.`ID_type_cost` AS `ID_type_cost`,`q_cost`.`tc_name` AS `tc_name`,`q_cost`.`c_price` AS `c_price`,`q_cost`.`c_note` AS `c_note`,`q_cost`.`c_delete` AS `c_delete`,`q_order_detail`.`ID_order_detail` AS `ID_order_detail`,`q_order_detail`.`ID_order` AS `ID_order`,`q_order_detail`.`o_customer_name` AS `o_customer_name`,`q_order_detail`.`ID_pro` AS `ID_pro`,`q_order_detail`.`p_name` AS `p_name`,`q_order_detail`.`ID_type` AS `ID_type`,`q_order_detail`.`t_name` AS `t_name`,`q_order_detail`.`ID_art` AS `ID_art`,`q_order_detail`.`a_name` AS `a_name`,`q_order_detail`.`ID_web` AS `ID_web`,`q_order_detail`.`w_name` AS `w_name`,`q_order_detail`.`ID_ver` AS `ID_ver`,`q_order_detail`.`v_name` AS `v_name`,`q_order_detail`.`ID_cover` AS `ID_cover`,`q_order_detail`.`c_name` AS `c_name`,`q_order_detail`.`c_price_total` AS `c_price_total`,`q_order_detail`.`c_price_pledge` AS `c_price_pledge`,`q_order_detail`.`c_price_balance` AS `c_price_balance`,`q_order_detail`.`od_qty` AS `od_qty`,`q_order_detail`.`od_price_total` AS `od_price_total`,`q_order_detail`.`od_price_pledge` AS `od_price_pledge`,`q_order_detail`.`od_price_balance` AS `od_price_balance`,`q_order_detail`.`ID_order_status` AS `ID_order_status`,`q_order_detail`.`os_name` AS `os_name` from ((`t_cost_detail` join `q_cost` on((`q_cost`.`ID_cost` = `t_cost_detail`.`ID_cost`))) join `q_order_detail` on((`q_order_detail`.`ID_order_detail` = `t_cost_detail`.`ID_order_detail`)));

-- ----------------------------
-- View structure for q_cover
-- ----------------------------
DROP VIEW IF EXISTS `q_cover`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_cover` AS select `t_cover`.`ID_cover` AS `ID_cover`,`t_cover`.`ID_pro` AS `ID_pro`,`t_product`.`p_name` AS `p_name`,`t_product`.`ID_type` AS `ID_type`,`t_type`.`t_name` AS `t_name`,`t_cover`.`ID_web` AS `ID_web`,`t_website`.`w_name` AS `w_name`,`t_cover`.`ID_ver` AS `ID_ver`,`t_version`.`v_name` AS `v_name`,`t_cover`.`c_name` AS `c_name`,`t_cover`.`c_price_total` AS `c_price_total`,`t_cover`.`c_price_pledge` AS `c_price_pledge`,`t_cover`.`c_price_balance` AS `c_price_balance`,`t_cover`.`c_delete` AS `c_delete` from ((((`t_cover` join `t_product` on((`t_product`.`ID_product` = `t_cover`.`ID_pro`))) join `t_website` on((`t_website`.`ID_web` = `t_cover`.`ID_web`))) join `t_version` on((`t_version`.`ID_ver` = `t_cover`.`ID_ver`))) join `t_type` on((`t_type`.`ID_type` = `t_product`.`ID_type`)));

-- ----------------------------
-- View structure for q_customer_name
-- ----------------------------
DROP VIEW IF EXISTS `q_customer_name`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_customer_name` AS select `t_order`.`o_customer_name` AS `o_customer_name` from `t_order` group by `t_order`.`o_customer_name`;

-- ----------------------------
-- View structure for q_end_date
-- ----------------------------
DROP VIEW IF EXISTS `q_end_date`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_end_date` AS select `t_product`.`p_end_date` AS `p_end_date`,cast(substr(`t_product`.`p_end_date`,1,2) as unsigned) AS `p_end_date_DAY`,cast(substr(`t_product`.`p_end_date`,4,2) as unsigned) AS `p_end_date_MONTH`,cast(substr(`t_product`.`p_end_date`,7,4) as unsigned) AS `p_end_date_YEAR`,`t_product`.`ID_pro_status` AS `ID_pro_status`,`t_product`.`p_delete` AS `p_delete` from `t_product`;

-- ----------------------------
-- View structure for q_group_website
-- ----------------------------
DROP VIEW IF EXISTS `q_group_website`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_group_website` AS select `t_cover`.`ID_web` AS `ID_web`,`t_website`.`w_name` AS `w_name`,`t_cover`.`ID_pro` AS `ID_pro`,`t_cover`.`c_delete` AS `c_delete` from (`t_cover` join `t_website` on((`t_website`.`ID_web` = `t_cover`.`ID_web`))) group by `t_cover`.`ID_web`,`t_website`.`w_name`,`t_cover`.`ID_pro`,`t_cover`.`c_delete`;

-- ----------------------------
-- View structure for q_income
-- ----------------------------
DROP VIEW IF EXISTS `q_income`;
CREATE ALGORITHM = UNDEFINED DEFINER = `sriundee_shop`@`localhost` SQL SECURITY DEFINER VIEW `q_income` AS select `t_income`.`ID_income` AS `ID_income`,`t_income`.`c_create_date` AS `c_create_date`,`t_income`.`c_customer_name` AS `c_customer_name`,`t_income`.`ID_type_income` AS `ID_type_income`,`t_type_income`.`ti_name` AS `ti_name`,`t_income`.`c_price` AS `c_price`,`t_income`.`c_note` AS `c_note`,`t_income`.`c_delete` AS `c_delete`,`t_income`.`ID_order` AS `ID_order` from (`t_income` join `t_type_income` on((`t_income`.`ID_type_income` = `t_type_income`.`ID_type_income`)));

-- ----------------------------
-- View structure for q_order
-- ----------------------------
DROP VIEW IF EXISTS `q_order`;
CREATE ALGORITHM = UNDEFINED DEFINER = `sriundee_shop`@`localhost` SQL SECURITY DEFINER VIEW `q_order` AS select `t_order`.`ID_order` AS `ID_order`,`t_order`.`o_order_date` AS `o_order_date`,`t_order`.`o_order_code` AS `o_order_code`,`t_order`.`o_customer_name` AS `o_customer_name`,`t_order`.`ID_pay_method` AS `ID_pay_method`,`t_payment_method`.`pm_name` AS `pm_name`,`t_order`.`o_send_cost` AS `o_send_cost`,`t_order`.`o_discount` AS `o_discount`,`t_order`.`o_price_total` AS `o_price_total`,`t_order`.`ID_pay_type` AS `ID_pay_type`,`t_payment_type`.`pt_name` AS `pt_name`,`t_order`.`o_price_pledge` AS `o_price_pledge`,`t_order`.`o_price_balance` AS `o_price_balance`,`t_order`.`o_last_pay_date` AS `o_last_pay_date`,`t_order`.`o_net` AS `o_net`,`t_order`.`o_remark` AS `o_remark` from ((`t_order` join `t_payment_method` on((`t_payment_method`.`ID_pay_method` = `t_order`.`ID_pay_method`))) left join `t_payment_type` on((`t_payment_type`.`ID_pay_type` = `t_order`.`ID_pay_type`)));

-- ----------------------------
-- View structure for q_order_detail
-- ----------------------------
DROP VIEW IF EXISTS `q_order_detail`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_order_detail` AS select `t_order_detail`.`ID_order_detail` AS `ID_order_detail`,`t_order_detail`.`ID_order` AS `ID_order`,`t_order`.`o_customer_name` AS `o_customer_name`,`t_cover`.`ID_pro` AS `ID_pro`,`t_product`.`p_name` AS `p_name`,`t_product`.`ID_type` AS `ID_type`,`t_type`.`t_name` AS `t_name`,`t_product`.`ID_art` AS `ID_art`,`t_artist`.`a_name` AS `a_name`,`t_cover`.`ID_web` AS `ID_web`,`t_website`.`w_name` AS `w_name`,`t_cover`.`ID_ver` AS `ID_ver`,`t_version`.`v_name` AS `v_name`,`t_order_detail`.`ID_cover` AS `ID_cover`,`t_cover`.`c_name` AS `c_name`,format(`t_cover`.`c_price_total`,0) AS `c_price_total`,format(`t_cover`.`c_price_pledge`,0) AS `c_price_pledge`,format(`t_cover`.`c_price_balance`,0) AS `c_price_balance`,`t_order_detail`.`od_qty` AS `od_qty`,format(`t_order_detail`.`od_price_total`,0) AS `od_price_total`,format(`t_order_detail`.`od_price_pledge`,0) AS `od_price_pledge`,format(`t_order_detail`.`od_price_balance`,0) AS `od_price_balance`,`t_order_detail`.`ID_order_status` AS `ID_order_status`,`t_order_status`.`os_name` AS `os_name` from ((((((((`t_order_detail` join `t_cover` on((`t_cover`.`ID_cover` = `t_order_detail`.`ID_cover`))) join `t_product` on((`t_product`.`ID_product` = `t_cover`.`ID_pro`))) join `t_website` on((`t_website`.`ID_web` = `t_cover`.`ID_web`))) join `t_version` on((`t_version`.`ID_ver` = `t_cover`.`ID_ver`))) join `t_type` on((`t_type`.`ID_type` = `t_product`.`ID_type`))) join `t_artist` on((`t_artist`.`ID_art` = `t_product`.`ID_art`))) join `t_order_status` on((`t_order_status`.`ID_order_status` = `t_order_detail`.`ID_order_status`))) left join `t_order` on((`t_order`.`ID_order` = `t_order_detail`.`ID_order`)));

-- ----------------------------
-- View structure for q_product
-- ----------------------------
DROP VIEW IF EXISTS `q_product`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_product` AS select `t_product`.`ID_product` AS `ID_product`,`t_product`.`p_name` AS `p_name`,`t_product`.`ID_type` AS `ID_type`,`t_type`.`t_name` AS `t_name`,`t_product`.`ID_art` AS `ID_art`,`t_artist`.`a_name` AS `a_name`,`t_artist`.`a_logo` AS `a_logo`,date_format(`t_product`.`p_end_date`,'%d/%m/%Y') AS `p_end_date`,date_format(`t_product`.`p_send_date`,'%d/%m/%Y') AS `p_send_date`,`t_product`.`ID_pro_status` AS `ID_pro_status`,`t_product_status`.`ps_name` AS `ps_name`,`t_product`.`p_delete` AS `p_delete`,`t_product`.`p_pic` AS `p_pic` from (((`t_product` join `t_type` on((`t_type`.`ID_type` = `t_product`.`ID_type`))) join `t_artist` on((`t_artist`.`ID_art` = `t_product`.`ID_art`))) join `t_product_status` on((`t_product_status`.`ID_pro_status` = `t_product`.`ID_pro_status`)));

-- ----------------------------
-- View structure for q_product_web
-- ----------------------------
DROP VIEW IF EXISTS `q_product_web`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_product_web` AS select `t_product_web`.`ID_pro_web` AS `ID_pro_web`,`t_product_web`.`ID_pro` AS `ID_pro`,`t_product`.`p_name` AS `p_name`,`t_product_web`.`ID_web` AS `ID_web`,`t_website`.`w_name` AS `w_name` from ((`t_product_web` join `t_product` on((`t_product`.`ID_product` = `t_product_web`.`ID_pro`))) join `t_website` on((`t_website`.`ID_web` = `t_product_web`.`ID_web`)));

-- ----------------------------
-- View structure for q_report_money_cost
-- ----------------------------
DROP VIEW IF EXISTS `q_report_money_cost`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_report_money_cost` AS select `t_cost`.`ID_cost` AS `ID_r`,`t_cost`.`c_create_date` AS `r_date`,`t_type_cost`.`tc_name` AS `r_name`,`t_cost`.`c_price` AS `r_price`,`t_cost`.`c_note` AS `r_note`,`t_cost`.`c_delete` AS `r_delete`,'C' AS `r_tpye` from (`t_cost` join `t_type_cost` on((`t_cost`.`ID_type_cost` = `t_type_cost`.`ID_type_cost`)));

-- ----------------------------
-- View structure for q_report_money_income
-- ----------------------------
DROP VIEW IF EXISTS `q_report_money_income`;
CREATE ALGORITHM = UNDEFINED DEFINER = `sriundee_shop`@`localhost` SQL SECURITY DEFINER VIEW `q_report_money_income` AS select `t_income`.`ID_income` AS `ID_r`,`t_income`.`c_create_date` AS `r_date`,`t_type_income`.`ti_name` AS `r_name`,`t_income`.`c_price` AS `r_price`,`t_income`.`c_note` AS `r_note`,`t_income`.`c_delete` AS `r_delete`,`t_income`.`ID_order` AS `ID_order`,'I' AS `r_tpye` from (`t_income` join `t_type_income` on((`t_income`.`ID_type_income` = `t_type_income`.`ID_type_income`)));

-- ----------------------------
-- View structure for q_send_date
-- ----------------------------
DROP VIEW IF EXISTS `q_send_date`;
CREATE ALGORITHM = UNDEFINED DEFINER = `root`@`localhost` SQL SECURITY DEFINER VIEW `q_send_date` AS select `t_product`.`p_send_date` AS `p_send_date`,cast(substr(`t_product`.`p_send_date`,1,2) as unsigned) AS `p_send_date_DAY`,cast(substr(`t_product`.`p_send_date`,4,2) as unsigned) AS `p_send_date_MONTH`,cast(substr(`t_product`.`p_send_date`,7,4) as unsigned) AS `p_send_date_YEAR`,`t_product`.`ID_pro_status` AS `ID_pro_status`,`t_product`.`p_delete` AS `p_delete` from `t_product`;

SET FOREIGN_KEY_CHECKS = 1;
