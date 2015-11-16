UserIdentityBackend
===================

![Build Status](http://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-UserIdentityBackend)


Stores UserIdentities and their relation to Roles, Applications and Organizations.
Requires SecurityTokenService if authorization is turned on. 


![Architectural Overview](https://wiki.cantara.no/download/attachments/37388694/Whydah+infrastructure.png)


Installation
============



* create a user for the service

* create update-service.sh
```
#!/bin/sh

A=UserIdentityBackend
V=SNAPSHOT


if [[ $V == *SNAPSHOT* ]]; then
   echo Note: If the artifact version contains "SNAPSHOT" - the artifact latest greates snapshot is downloaded, Irrelevent of version number!!!
   path="http://mvnrepo.cantara.no/content/repositories/snapshots/net/whydah/identity/$A"
   version=`curl -s "$path/maven-metadata.xml" | grep "<version>" | sed "s/.*<version>\([^<]*\)<\/version>.*/\1/" | tail -n 1`
   echo "Version $version"
   build=`curl -s "$path/$version/maven-metadata.xml" | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
   JARFILE="$A-$build.jar"
   url="$path/$version/$JARFILE"
else #A specific Release version
   path="http://mvnrepo.cantara.no/content/repositories/releases/net/whydah/identity/$A"
   url=$path/$V/$A-$V.jar
   JARFILE=$A-$V.jar
fi

# Download
echo Downloading $url
wget -O $JARFILE -q -N $url


#Create symlink or replace existing sym link
if [ -h $A.jar ]; then
   unlink $A.jar
fi
ln -s $JARFILE $A.jar
```




* create useridentitybackend.TEST.properties - TODO Decide which mode should be described here...

```
DEFCON=5
# Normal operations
prop.type=DEV
ldap.embedded=true
ldap.embedded.port=11389
ldap.embedded.directory=target/bootstrapdata/ldap

ldap.primary.url=ldap://localhost:11389/dc=external,dc=WHYDAH,dc=no
ldap.primary.admin.principal=uid=admin,ou=system
ldap.primary.admin.credentials=secret
ldap.primary.uid.attribute=uid
ldap.primary.username.attribute=initials
ldap.primary.readonly=false

roledb.directory=target/bootstrapdata/hsqldb
roledb.jdbc.driver=org.hsqldb.jdbc.JDBCDriver
roledb.jdbc.url=jdbc:hsqldb:file:target/bootstrapdata/hsqldb/roles
roledb.jdbc.user=sa
roledb.jdbc.password=

import.enabled=true
import.usersource=testdata/users.csv
import.rolemappingsource=testdata/rolemappings.csv
import.applicationssource=testdata/applications.csv
import.organizationssource=testdata/organizations.csv

useradmin.requiredrolename=WhydahUserAdmin

adduser.defaultrole.name=WhydahDefaultUser
adduser.defaultrole.value=true
adduser.defaultapplication.name=WhydahTestWebApplication
adduser.defaultapplication.id=99
adduser.defaultorganization.name=Whydah

adduser.netiq.defaultrole.name=Employee
adduser.netiq.defaultrole.value=$email  // Not used placeholder
adduser.netiq.defaultapplication.name=ACS
adduser.netiq.defaultapplication.id=100
adduser.netiq.defaultorganization.name=ACSOrganization

adduser.facebook.defaultrole.name=FBData
adduser.facebook.defaultrole.value=$fbdata  // Not used placeholder
adduser.facebook.defaultapplication.name=WhydahTestWebApplication
adduser.facebook.defaultapplication.id=99
adduser.facebook.defaultorganization.name=Facebook

securitytokenservice=mock
ssologinservice=http://localhost:9997/sso/
myuri=http://localhost:9995/uib/
service.port=9995
lucene.directory=target/bootstrapdata/lucene
```


* create start-service.sh (normal/prod mode)
```
#!/bin/bash
# Used by https://github.com/Cantara/Whydah/tree/master/dev-quickstart

# If Version is from source, find the artifact
if [ "$Version" = "FROM_SOURCE" ]; then
    # Find the bult artifact
    Version=$(find target/* -name '*.jar' | grep SNAPSHOT | grep -v original | grep -v lib)
else
    Version=UserAdminService.jar
fi


nohup /usr/bin/java $env_vars -jar  $Version &
```



Typical apache setup
====================

```
<VirtualHost *:80>
        ServerName myserver.net
        ServerAlias myserver
        ProxyRequests Off
        <Proxy *>
                Order deny,allow
                Allow from all
        </Proxy>
        ProxyPreserveHost on
                ProxyPass /sso http://localhost:9997/sso
                ProxyPass /uib http://localhost:9995/uib
                ProxyPass /tokenservice http://localhost:9998/tokenservice
                ProxyPass /useradmin http://localhost:9996/useradmin
                ProxyPass /test http://localhost:9990/test/
</VirtualHost>
```



Developer info
==============

* https://wiki.cantara.no/display/iam/Architecture+Overview
* https://wiki.cantara.no/display/iam/Key+Whydah+Data+Structures
* https://wiki.cantara.no/display/iam/Modules

If you are planning on integrating, you might want to run SecurityTokenService in DEV mode. This shortcuts the authentication.
You can manually control the UserTokens for the different test-users you want, by creating a file named t_<username>.token which
consists of the XML representation of the access roles++ you want the spesific user to expose to the integrated application.

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
