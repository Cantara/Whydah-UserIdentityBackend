#!/bin/sh
echo Kaller UserIdentityBackend, skal gå bra:
curl -d "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>super</username><coffee>yes please</coffee><password>duper</password></user></auth></authgreier>" -H "Content-Type: application/xml" http://localhost:9995/uib/logon -o super.xml
