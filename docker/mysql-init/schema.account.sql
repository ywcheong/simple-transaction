-- account
CREATE TABLE account
(
    id              VARCHAR(64) NOT NULL PRIMARY KEY,
    owner           VARCHAR(64) NOT NULL, -- MSA 염두해 Foreign Key 제약은 제거
    is_withdrew     BOOLEAN     NOT NULL,
    balance         BIGINT      NOT NULL,
    pending_balance BIGINT      NOT NULL,
    version         BIGINT      NOT NULL, -- 낙관 락 버전
    INDEX idx_id (id),
    INDEX idx_owner (owner)
);

-- account_event
CREATE TABLE account_event
(
    id            VARCHAR(64) NOT NULL PRIMARY KEY,
    event_type    TINYINT     NOT NULL,
    account       VARCHAR(64),
    account_from  VARCHAR(64),
    account_to    VARCHAR(64),
    amount        BIGINT,
    subsequent_id VARCHAR(64),
    reason        VARCHAR(255),
    issued_at     DATETIME    NOT NULL,
    issued_by     VARCHAR(64) NOT NULL,
    published_at  DATETIME,
    INDEX idx_account (account),
    INDEX idx_account_from (account_from),
    INDEX idx_account_to (account_to),
    INDEX idx_event_type (event_type),
    INDEX idx_issued_at (issued_at),
    INDEX idx_subsequent_id (subsequent_id)
);