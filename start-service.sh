#!/bin/bash

#  If IAM_MODE not set, use PROD
if [ -z "$IAM_MODE" ]; then 
  IAM_MODE=PROD
fi


# If Version is from source, find the artifact
if [ "$Version" = "FROM_SOURCE" ]; then 
    # Find the bult artifact
    Version=$(find target/* -name '*.jar' | grep SNAPSHOT | grep -v original | grep -v lib)
else
    Version=UserIdentityBackend.jar
fi

# If IAM_CONFIG not set, use embedded
if [ -z "$IAM_CONFIG" ]; then
  nohup /usr/bin/java -DIAM_MODE=$IAM_MODE   -jar  $Version &
else  
  nohup /usr/bin/java -DIAM_MODE=$IAM_MODE  -DIAM_CONFIG=$IAM_CONFIG -jar  $Version &
fi

