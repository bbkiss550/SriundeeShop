-- Clears financial/order flow data and removes FLOW50 master records made by
-- scripts/test/full-flow-50-transactions.ps1.
-- Review before running on a database that contains real orders.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `t_cost_detail`;
TRUNCATE TABLE `t_lot_detail`;
TRUNCATE TABLE `t_income`;
TRUNCATE TABLE `t_cost`;
TRUNCATE TABLE `t_lot`;
TRUNCATE TABLE `t_order_detail`;
TRUNCATE TABLE `t_order`;

DELETE FROM `t_cover`
WHERE `c_name` LIKE 'FLOW50-%';

DELETE FROM `t_product_web`
WHERE `ID_pro` IN (
    SELECT `ID_product`
    FROM `t_product`
    WHERE `p_name` LIKE 'FLOW50-%'
);

DELETE FROM `t_version`
WHERE `v_name` LIKE 'FLOW50-%';

DELETE FROM `t_product`
WHERE `p_name` LIKE 'FLOW50-%';

DELETE FROM `t_artist`
WHERE `a_name` LIKE 'FLOW50-%';

DELETE FROM `t_type`
WHERE `t_name` LIKE 'FLOW50-%';

DELETE FROM `t_website`
WHERE `w_name` LIKE 'FLOW50-%';

SET FOREIGN_KEY_CHECKS = 1;
