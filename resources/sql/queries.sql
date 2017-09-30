-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(email, password, time_created)
VALUES (:email, :password, :timestamp)

-- :name update-password! :! :n
-- :doc update a user's password
UPDATE users
SET password = :newpassword
WHERE email = :email AND password = :password

-- :name is-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE email = :email AND password = :password

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id
