CREATE TABLE teller
(
    id                VARCHAR(64) NOT NULL PRIMARY KEY,
    type              INT         NOT NULL,
    active            BOOLEAN     NOT NULL DEFAULT true,
    public_key        VARCHAR(256),
    version           LONG        NOT NULL
);

CREATE TABLE teller_permission
(
    id         VARCHAR(64) NOT NULL,
    permission VARCHAR(64) NOT NULL,
    FOREIGN KEY (id) REFERENCES teller (id) ON DELETE CASCADE
);

CREATE TABLE auto_teller
(
    id      VARCHAR(64) NOT NULL PRIMARY KEY,
    vault   LONG        NOT NULL,
    version LONG        NOT NULL,
    FOREIGN KEY (id) REFERENCES teller (id) ON DELETE CASCADE
);