#!/bin/sh
nohup /usr/bin/java -DIAM_MODE=PROD -DIAM_CONFIG=/home/UserIdentityBackend/useridentitybackend.PROD.properties -jar /home/UserIdentityBackend/UserIdentityBackend.jar &
