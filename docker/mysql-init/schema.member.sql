-- member 테이블 생성
CREATE TABLE member
(
    id       VARCHAR(64)  NOT NULL PRIMARY KEY,
    name     VARCHAR(64)  NOT NULL,
    phone    VARCHAR(64)  NOT NULL,
    password VARCHAR(192) NOT NULL,
    status   INT          NOT NULL,
    UNIQUE INDEX idx_name (name)
);