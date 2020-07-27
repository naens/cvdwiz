#!/bin/dash

sqlfile=cvd.sql

sqlite3 $sqlfile << EOF
CREATE TABLE IF NOT EXISTS country (
    id INTEGER PRIMARY KEY,
    key TEXT UNIQUE,
    name TEXT,
    continent TEXT,
    population INTEGER
);

CREATE TABLE IF NOT EXISTS data (
    day TEXT,
    country INTEGER,
    new_cases INTEGER,
    PRIMARY KEY(day, country),
    FOREIGN KEY (country) REFERENCES country(id)
);

EOF
