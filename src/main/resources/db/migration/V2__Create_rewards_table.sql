CREATE TABLE rewards
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    reward_name  VARCHAR(255) NOT NULL,
    receipt_date DATE         NOT NULL,
    employee_id  BIGINT       NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rewards_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_rewards_employee_id ON rewards (employee_id);
CREATE INDEX idx_rewards_receipt_date ON rewards (receipt_date);