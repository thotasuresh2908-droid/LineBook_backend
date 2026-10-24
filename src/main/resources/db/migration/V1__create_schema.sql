-- LineBook schema: one database, one table group per module.
-- Foreign keys connect the modules (customers -> agents, bills -> customers, collections -> bills, ...).

-- ------------------------------------------------------------------ customer module
CREATE TABLE package_plans (
    id          VARCHAR(64)    NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    sort_order  INT            NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ------------------------------------------------------------------ agent module
CREATE TABLE agents (
    id          VARCHAR(64)  NOT NULL,                   -- ag-01, ag-02, ...
    seq         BIGINT       NOT NULL AUTO_INCREMENT,    -- creation order
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(15)  NOT NULL,
    area        VARCHAR(100) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agents_seq (seq),
    KEY idx_agents_active (active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ------------------------------------------------------------------ customer module
CREATE TABLE customers (
    id            VARCHAR(64)    NOT NULL,
    seq           BIGINT         NOT NULL AUTO_INCREMENT,   -- creation order
    account_no    VARCHAR(32)    NOT NULL,
    name          VARCHAR(150)   NOT NULL,
    phone         VARCHAR(15)    NOT NULL,
    address       VARCHAR(500)   NOT NULL DEFAULT '',
    area          VARCHAR(100)   NOT NULL,
    stb_no        VARCHAR(32)    NOT NULL,
    vc_no         VARCHAR(32)    NOT NULL,
    package_id    VARCHAR(64)    NOT NULL,
    monthly_rate  DECIMAL(12, 2) NOT NULL,
    status        VARCHAR(16)    NOT NULL,                   -- PENDING | ACTIVE | SUSPENDED | DISCONNECTED
    recurring     BOOLEAN        NOT NULL,
    billing_day   INT            NOT NULL DEFAULT 1,
    agent_id      VARCHAR(64)    NOT NULL,
    since_date    DATE           NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_seq (seq),
    UNIQUE KEY uk_customers_account_no (account_no),
    KEY idx_customers_area (area),
    KEY idx_customers_status (status),
    KEY idx_customers_agent (agent_id),
    CONSTRAINT fk_customers_package FOREIGN KEY (package_id) REFERENCES package_plans (id),
    CONSTRAINT fk_customers_agent   FOREIGN KEY (agent_id)   REFERENCES agents (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ------------------------------------------------------------------ billing module
CREATE TABLE bills (
    id            VARCHAR(64)    NOT NULL,
    bill_no       VARCHAR(32)    NOT NULL,
    customer_id   VARCHAR(64)    NOT NULL,
    cycle         VARCHAR(7)     NOT NULL,           -- yyyy-MM
    amount        DECIMAL(12, 2) NOT NULL,
    generated_at  DATETIME       NOT NULL,
    due_date      DATE           NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bills_bill_no (bill_no),
    UNIQUE KEY uk_bills_customer_cycle (customer_id, cycle),
    KEY idx_bills_cycle (cycle),
    CONSTRAINT fk_bills_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE bill_payments (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    bill_id        VARCHAR(64)    NOT NULL,
    collection_id  VARCHAR(64)    NOT NULL,          -- id of the collection (idempotency key)
    amount         DECIMAL(12, 2) NOT NULL,
    paid_at        DATETIME       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_payments_collection (collection_id),
    KEY idx_bill_payments_bill (bill_id),
    CONSTRAINT fk_bill_payments_bill FOREIGN KEY (bill_id) REFERENCES bills (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ------------------------------------------------------------------ collection module
-- One row per day; the next receipt number is taken atomically (see MySqlReceiptNumbers).
CREATE TABLE receipt_counters (
    receipt_date  DATE NOT NULL,
    last_seq      INT  NOT NULL,
    PRIMARY KEY (receipt_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Bill / customer / agent details are copied onto each row so a receipt never changes after it is issued.
CREATE TABLE collections (
    id             VARCHAR(64)    NOT NULL,
    receipt_no     VARCHAR(32)    NOT NULL,           -- RC-yyyyMMdd-0001
    bill_id        VARCHAR(64)    NOT NULL,
    bill_no        VARCHAR(32)    NOT NULL,
    customer_id    VARCHAR(64)    NOT NULL,
    customer_name  VARCHAR(150)   NOT NULL,
    account_no     VARCHAR(32)    NOT NULL,
    stb_no         VARCHAR(32)    NOT NULL,
    agent_id       VARCHAR(64)    NOT NULL,
    agent_name     VARCHAR(150)   NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    method         VARCHAR(8)     NOT NULL,           -- CASH | UPI
    collected_at   DATETIME       NOT NULL,
    notes          VARCHAR(500)   NOT NULL DEFAULT '',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_collections_receipt_no (receipt_no),
    KEY idx_collections_collected_at (collected_at),
    KEY idx_collections_agent (agent_id, collected_at),
    KEY idx_collections_bill (bill_id),
    KEY idx_collections_customer (customer_id),
    CONSTRAINT fk_collections_bill     FOREIGN KEY (bill_id)     REFERENCES bills (id),
    CONSTRAINT fk_collections_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_collections_agent    FOREIGN KEY (agent_id)    REFERENCES agents (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
