-- member 테이블 생성
CREATE TABLE member
(
    id       VARCHAR(36)  NOT NULL,
    name     VARCHAR(16)  NOT NULL, -- member.name is unique: INDEX idx_member_name
    phone    VARCHAR(32)  NOT NULL,
    password VARCHAR(256) NOT NULL,
    status   INT          NOT NULL,
    PRIMARY KEY (id)
);

-- member 이름에 대한 유니크 인덱스
CREATE UNIQUE INDEX idx_member_name ON member (name);

-- 애플리케이션에 권한 부여
use stdb;
GRANT SELECT, INSERT, UPDATE, DELETE ON member TO 'stserver'@'%';
FLUSH PRIVILEGES;
