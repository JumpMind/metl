CREATE TABLE "METL_RELATIONAL_MODEL"(
    "ID" CHAR(36) NOT NULL,
    "ROW_ID" CHAR(36) NOT NULL,
    "PROJECT_VERSION_ID" CHAR(36) NULL,
    "DELETED" TINYINT DEFAULT 0 NOT NULL,
    "NAME" VARCHAR(255) NULL,
    "SHARED" TINYINT DEFAULT 0,
    "FOLDER_ID" CHAR(36) NULL,
    "CREATE_TIME" TIMESTAMP,
    "CREATE_BY" VARCHAR(50) NULL,
    "LAST_UPDATE_BY" VARCHAR(50) NULL,
    "LAST_UPDATE_TIME" TIMESTAMP,
    PRIMARY KEY ("ID")
);
CREATE INDEX "METL_IDX_REL_MODEL_PV" ON "METL_RELATIONAL_MODEL" ("DELETED", "PROJECT_VERSION_ID");
ALTER TABLE "METL_RELATIONAL_MODEL"
    ADD CONSTRAINT "METL_FK_RM_2_FLDR" FOREIGN KEY ("FOLDER_ID") REFERENCES "METL_FOLDER" ("ID");
ALTER TABLE "METL_RELATIONAL_MODEL"
    ADD CONSTRAINT "METL_FK_RM_2_V" FOREIGN KEY ("PROJECT_VERSION_ID") REFERENCES "METL_PROJECT_VERSION" ("ID");


CREATE TABLE "METL_HIERARCHICAL_MODEL"(
    "ID" CHAR(36) NOT NULL,
    "ROW_ID" CHAR(36) NOT NULL,
    "PROJECT_VERSION_ID" CHAR(36) NULL,
    "DELETED" TINYINT DEFAULT 0 NOT NULL,
    "NAME" VARCHAR(255) NULL,
    "SHARED" TINYINT DEFAULT 0,
    "FOLDER_ID" CHAR(36) NULL,
    "CREATE_TIME" TIMESTAMP,
    "CREATE_BY" VARCHAR(50) NULL,
    "LAST_UPDATE_BY" VARCHAR(50) NULL,
    "LAST_UPDATE_TIME" TIMESTAMP,
    PRIMARY KEY ("ID")
);
CREATE INDEX "METL_IDX_HIER_MODEL_PV" ON "METL_HIERARCHICAL_MODEL" ("DELETED", "PROJECT_VERSION_ID");
ALTER TABLE "METL_HIERARCHICAL_MODEL"
    ADD CONSTRAINT "METL_FK_HM_2_FLDR" FOREIGN KEY ("FOLDER_ID") REFERENCES "METL_FOLDER" ("ID");
ALTER TABLE "METL_HIERARCHICAL_MODEL"
    ADD CONSTRAINT "METL_FK_HM_2_V" FOREIGN KEY ("PROJECT_VERSION_ID") REFERENCES "METL_PROJECT_VERSION" ("ID");


insert into metl_hierarchical_model (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, SHARED, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) 
select ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, SHARED, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME from metl_model where upper(type)='HIERARCHICAL';

insert into metl_relational_model (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, SHARED, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) 
select ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, SHARED, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME from metl_model where upper(type)='RELATIONAL';
