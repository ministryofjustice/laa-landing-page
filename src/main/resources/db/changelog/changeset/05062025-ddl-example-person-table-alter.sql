--liquibase formatted sql
--changeset pliao:05062025-ddl-example-person-table-alter
ALTER TABLE Persons
    ADD Email varchar(255);