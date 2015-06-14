#!/bin/bash

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

