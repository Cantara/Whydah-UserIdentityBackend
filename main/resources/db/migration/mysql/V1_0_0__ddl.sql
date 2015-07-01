CREATE TABLE UserRoles (
  RoleID char(255),
  UserID char(255) NOT NULL,
  AppID varchar(255),
  OrganizationName varchar(255),
  RoleName varchar(255),
  RoleValues varchar(256),
  PRIMARY KEY (RoleID)
);

CREATE TABLE Applications (
  ID varchar(32),
  Name varchar(255),
  DefaultRoleName varchar(256) default null,
  DefaultOrgName varchar(256) default null,
  ApplicationSecret varchar(256) default null,
  PRIMARY KEY(ID)
);

CREATE TABLE Organization (
  ID varchar(32),
  Name varchar(255)
);

CREATE TABLE Roles (
  ID char(32),
  Name varchar(255)
);

CREATE TABLE AUDITLOG (
  ID MEDIUMINT NOT NULL AUTO_INCREMENT,
  userid varchar(255),
  timestamp varchar(20),
  action varchar(255),
  field varchar(255),
  value varchar(4096),
  PRIMARY KEY(ID)
);