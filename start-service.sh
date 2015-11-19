#!/bin/bash
# Used by https://github.com/Cantara/Whydah/tree/master/dev-quickstart

# If Version is from source, find the artifact
if [ "$Version" = "FROM_SOURCE" ]; then 
    # Find the bult artifact
    Version=$(find target/* -name '*.jar' | grep SNAPSHOT | grep -v original | grep -v lib)
else
    Version=UserAdminService.jar
fi


nohup /usr/bin/java $env_vars  -DCONSTRETTO_TAGS=DEV -jar  $Version &
