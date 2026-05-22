INSERT INTO `t_menu` (`ID_menu`, `m_name`, `m_parent`, `m_ID_menu`, `m_url`, `m_icon`, `m_order`)
SELECT 17, 'คำนวณราคาขาย', 'N', NULL, '/pricing-calculator', 'bi bi-calculator', 13
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_menu`
    WHERE `ID_menu` = 17
       OR `m_url` = '/pricing-calculator'
);
