ALTER TABLE `t_menu`
ADD COLUMN `m_order` int(0) DEFAULT NULL AFTER `m_icon`;

UPDATE `t_menu` SET `m_order` = 2 WHERE `ID_menu` = 2;
UPDATE `t_menu` SET `m_order` = 1 WHERE `ID_menu` = 3;
UPDATE `t_menu` SET `m_order` = 2 WHERE `ID_menu` = 4;
UPDATE `t_menu` SET `m_order` = 3 WHERE `ID_menu` = 5;
UPDATE `t_menu` SET `m_order` = 3 WHERE `ID_menu` = 6;
UPDATE `t_menu` SET `m_order` = 4 WHERE `ID_menu` = 7;
UPDATE `t_menu` SET `m_order` = 7 WHERE `ID_menu` = 8;
UPDATE `t_menu` SET `m_order` = 8 WHERE `ID_menu` = 9;
UPDATE `t_menu` SET `m_order` = 9 WHERE `ID_menu` = 10;
UPDATE `t_menu` SET `m_order` = 10 WHERE `ID_menu` = 11;
UPDATE `t_menu` SET `m_order` = 5 WHERE `ID_menu` = 12;
UPDATE `t_menu` SET `m_order` = 6 WHERE `ID_menu` = 13;
UPDATE `t_menu` SET `m_order` = 1 WHERE `ID_menu` = 14;
UPDATE `t_menu` SET `m_order` = 11 WHERE `ID_menu` = 15;
UPDATE `t_menu` SET `m_order` = 12 WHERE `ID_menu` = 16;
