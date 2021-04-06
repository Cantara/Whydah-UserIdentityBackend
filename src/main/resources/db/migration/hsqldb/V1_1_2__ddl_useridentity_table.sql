CREATE TABLE UserIdentity (
    id        varchar(100) NOT NULL,
    username  varchar(255) NOT NULL,
    firstname varchar(255) NOT NULL,
    lastname  varchar(255) NOT NULL,
    personref varchar(100),
    email     varchar(255) NOT NULL,
    cellphone varchar(20)  NOT NULL,
    password  varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (username)
);
