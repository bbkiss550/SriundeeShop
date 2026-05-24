ALTER TABLE `t_settings`
    MODIFY COLUMN `s_value` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

INSERT INTO `t_menu` (`ID_menu`, `m_name`, `m_parent`, `m_ID_menu`, `m_url`, `m_icon`, `m_order`)
SELECT 18, 'จัดการ Dashboard', 'N', NULL, '/dashboard/manage', 'bi bi-grid-1x2', 14
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_menu`
    WHERE `ID_menu` = 18
       OR `m_url` = '/dashboard/manage'
);

INSERT INTO `t_settings` (`s_key`, `s_value`)
SELECT 'dashboard_widgets', '[]'
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_settings`
    WHERE `s_key` = 'dashboard_widgets'
);
