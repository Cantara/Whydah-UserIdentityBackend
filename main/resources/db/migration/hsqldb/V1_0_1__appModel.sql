DROP TABLE Roles IF EXISTS;
DROP TABLE Applications IF EXISTS;


CREATE TABLE Application (
  ID varchar(64) PRIMARY KEY,
  Name varchar(255),
  Secret varchar(255) default null,
  AvailableOrgNames varchar(255) default null,
  DefaultRoleName varchar(150) default null,
  DefaultOrgName varchar(150) default null
);

CREATE TABLE Role (
  ID char(32) PRIMARY KEY,
  Name varchar(255),
  applicationId varchar(32)
  --FOREIGN KEY (ApplicationId) REFERENCES Application(ID)
  --FOREIGN KEY (Application) REFERENCES Application
);