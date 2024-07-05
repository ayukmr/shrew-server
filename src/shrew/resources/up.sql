CREATE TABLE points (
  id      INTEGER AUTO_INCREMENT PRIMARY KEY,
  scout   VARCHAR(255),
  event   VARCHAR(255),
  match   INTEGER,
  team    INTEGER,
  move    REAL,
  intake  REAL,
  outtake REAL,
  point   VARCHAR(255)
);

CREATE TABLE responses (
  id       INTEGER AUTO_INCREMENT PRIMARY KEY,
  scout    VARCHAR(255),
  event    VARCHAR(255),
  match    INTEGER,
  team     INTEGER,
  type     VARCHAR(255),
  question VARCHAR(255),
  response VARCHAR(255)
);

CREATE TABLE auth (
  team  INTEGER PRIMARY KEY,
  scout VARCHAR(255),
  admin VARCHAR(255)
);

CREATE TABLE questions (
  id   INTEGER AUTO_INCREMENT PRIMARY KEY,
  team INTEGER,
  type VARCHAR(255),
  questions VARCHAR(255) ARRAY
);

CREATE TABLE settings (
  team   INTEGER PRIMARY KEY,
  event  VARCHAR(255),
  points VARCHAR(255) ARRAY
);
