-- Clears STATUS20 order/status test data made by
-- scripts/test/status-change-20-transactions.ps1.
-- Review before running on a database that contains real STATUS20-prefixed data.

SET FOREIGN_KEY_CHECKS = 0;

DELETE cd
FROM `t_cost_detail` cd
JOIN `t_order_detail` od ON od.`ID_order_detail` = cd.`ID_order_detail`
JOIN `t_order` o ON o.`ID_order` = od.`ID_order`
WHERE o.`o_remark` LIKE 'STATUS20-%';

DELETE ld
FROM `t_lot_detail` ld
JOIN `t_order_detail` od ON od.`ID_order_detail` = ld.`ID_order_detail`
JOIN `t_order` o ON o.`ID_order` = od.`ID_order`
WHERE o.`o_remark` LIKE 'STATUS20-%';

DELETE i
FROM `t_income` i
JOIN `t_order` o ON o.`ID_order` = i.`ID_order`
WHERE o.`o_remark` LIKE 'STATUS20-%';

DELETE FROM `t_cost`
WHERE `c_note` LIKE 'STATUS20-%';

DELETE FROM `t_lot`
WHERE `l_lot_number` LIKE 'STATUS20-%';

DELETE od
FROM `t_order_detail` od
JOIN `t_order` o ON o.`ID_order` = od.`ID_order`
WHERE o.`o_remark` LIKE 'STATUS20-%';

DELETE FROM `t_order`
WHERE `o_remark` LIKE 'STATUS20-%';

DELETE FROM `t_cover`
WHERE `c_name` LIKE 'STATUS20-%';

DELETE FROM `t_product_web`
WHERE `ID_pro` IN (
    SELECT `ID_product`
    FROM `t_product`
    WHERE `p_name` LIKE 'STATUS20-%'
);

DELETE FROM `t_version`
WHERE `v_name` LIKE 'STATUS20-%';

DELETE FROM `t_product`
WHERE `p_name` LIKE 'STATUS20-%';

DELETE FROM `t_artist`
WHERE `a_name` LIKE 'STATUS20-%';

DELETE FROM `t_type`
WHERE `t_name` LIKE 'STATUS20-%';

DELETE FROM `t_website`
WHERE `w_name` LIKE 'STATUS20-%';

SET FOREIGN_KEY_CHECKS = 1;
