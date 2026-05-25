ALTER TABLE `t_artist`
    MODIFY COLUMN `ID_group` int(0) DEFAULT NULL;

UPDATE `t_artist`
SET `ID_group` = 5
WHERE `ID_group` IS NULL
  AND `a_name` LIKE 'FLOW50-%';

UPDATE `t_artist`
SET `ID_group` = 5
WHERE `ID_group` IS NULL
  AND `a_name` LIKE 'STATUS20-%';
