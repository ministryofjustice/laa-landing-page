--liquibase formatted sql
--changeset pliao:05062025-dml-example-person-insert

INSERT INTO Persons
(PersonID, LastName, FirstName, Address)
VALUES(1, 'Last', 'Test', 'somewhere');