DELIMITER $$

CREATE PROCEDURE create_aux_tables()
BEGIN
  DECLARE i INT DEFAULT 1;

  WHILE i <= 50 DO

    SET @sql = CONCAT(
      'CREATE TABLE aux_table_', i, ' (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        tenant_id BIGINT,
        user_id BIGINT,
        order_id BIGINT,
        random_code VARCHAR(50),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      ) ENGINE=InnoDB;'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET i = i + 1;
END WHILE;

END$$

DELIMITER ;

CALL create_aux_tables();
DROP PROCEDURE create_aux_tables;