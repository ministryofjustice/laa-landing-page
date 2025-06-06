--liquibase formatted sql
--changeset pliao:05062025-ddl-example-person-table-create

CREATE TABLE Persons (
    PersonID int,
    LastName varchar(255),
    FirstName varchar(255),
    Address varchar(255)
);