CREATE TABLE users
(id SERIAL PRIMARY KEY,
email VARCHAR(30),
admin BOOLEAN,
last_login TIME,
time_created TIMESTAMP,
password VARCHAR(300));
