# Initial Schema

# --- !Ups

CREATE TABLE camera (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE sound_event (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    camera_id bigint(20) NOT NULL,
    start_date DATETIME NOT NULL UNIQUE,
    duration bigint(20),
    PRIMARY KEY (id)
);

# --- !Downs
