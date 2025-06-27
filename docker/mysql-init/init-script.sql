CREATE TABLE Member (
    id VARCHAR(36) NOT NULL ,
    name VARCHAR(16) NOT NULL, -- MEMBER.NAME is unique: INDEX idx_member_name
    phone VARCHAR(32) NOT NULL ,
    password VARCHAR(256) NOT NULL ,
    status INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_member_name ON Member(name);

GRANT SELECT, INSERT, UPDATE, DELETE ON stdb.Member TO 'stserver'@'%';
FLUSH PRIVILEGES;