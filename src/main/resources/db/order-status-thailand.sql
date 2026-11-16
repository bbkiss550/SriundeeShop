-- One atomic migration. ID 6 and the complete expected labels identify the new mapping.
-- Existing 3/4/5 orders keep their workflow position by moving to 4/5/6.
DO $migration$
BEGIN
    LOCK TABLE t_order_status, t_order_detail IN EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM t_order_status WHERE id_order_status = 6) THEN
        IF (SELECT COUNT(*) FROM t_order_status WHERE
            (id_order_status, os_name) IN (
                (0, 'ยกเลิก'), (1, 'รอกดของ'), (2, 'รอเว็บส่งของ'),
                (3, 'รอของถึงไทย'), (4, 'รอของถึงร้าน'),
                (5, 'ถึงร้านแล้วรอส่ง'), (5, 'รอส่ง'), (5, 'ถึงร้านแล้ว รอส่ง'),
                (6, 'ส่งของสำเร็จ'), (6, 'ส่งแล้ว'))) <> 7 THEN
            RAISE EXCEPTION 'Unexpected order status mapping; migration stopped';
        END IF;
        UPDATE t_order_status SET os_name = 'ถึงร้านแล้ว รอส่ง' WHERE id_order_status = 5;
        UPDATE t_order_status SET os_name = 'ส่งแล้ว' WHERE id_order_status = 6;
        UPDATE t_order_status SET os_color = 'order-status-' || id_order_status
            WHERE id_order_status BETWEEN 0 AND 6;
        RETURN;
    END IF;
    IF (SELECT COUNT(*) FROM t_order_status WHERE
        (id_order_status, os_name) IN (
            (0, 'ยกเลิก'), (1, 'รอกดของ'), (2, 'กดของแล้ว รอเข้าโกดัง'),
            (3, 'รอของถึงร้าน'), (4, 'ถึงร้านแล้วรอส่ง'), (5, 'ส่งของสำเร็จ'))) <> 6
        OR EXISTS (SELECT 1 FROM t_order_status WHERE id_order_status NOT BETWEEN 0 AND 5)
        OR EXISTS (SELECT 1 FROM t_order_detail WHERE id_order_status NOT BETWEEN 0 AND 5) THEN
        RAISE EXCEPTION 'Unexpected legacy order status mapping; migration stopped';
    END IF;

    INSERT INTO t_order_status (id_order_status, os_name, os_color)
        SELECT 6, 'ส่งของสำเร็จ', os_color FROM t_order_status WHERE id_order_status = 5;
    -- One UPDATE avoids shifting a row twice, including cancelled orders' historical details.
    UPDATE t_order_detail SET id_order_status = id_order_status + 1
        WHERE id_order_status BETWEEN 3 AND 5;
    UPDATE t_order_status s SET os_name = v.name, os_color = v.color
    FROM (SELECT 2 AS id, 'รอเว็บส่งของ' AS name, os_color AS color FROM t_order_status WHERE id_order_status = 2
          UNION ALL SELECT 3, 'รอของถึงไทย', 'info'
          UNION ALL SELECT 4, 'รอของถึงร้าน', os_color FROM t_order_status WHERE id_order_status = 3
          UNION ALL SELECT 5, 'ถึงร้านแล้วรอส่ง', os_color FROM t_order_status WHERE id_order_status = 4) v
    WHERE s.id_order_status = v.id;
    UPDATE t_order_status SET os_name = 'ถึงร้านแล้ว รอส่ง' WHERE id_order_status = 5;
    UPDATE t_order_status SET os_name = 'ส่งแล้ว' WHERE id_order_status = 6;
    UPDATE t_order_status SET os_color = 'order-status-' || id_order_status
        WHERE id_order_status BETWEEN 0 AND 6;
END
$migration$;
