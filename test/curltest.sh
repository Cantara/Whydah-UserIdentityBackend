#!/bin/sh
echo Kaller UserIdentityBackend, skal gå bra:
curl -d "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>ben@hotmail.com</username><coffee>yes please</coffee><password>061073</password></user></auth></authgreier>" -H "Content-Type: application/xml" http://localhost:9995/uib/logon
echo
echo
echo Kaller UserIdentityBackend, skal feile:
curl -d "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>ben@hotmail.com</username><coffee>yes please</coffee><password>vrangt</password></user></auth></authgreier>" -H "Content-Type: application/xml" http://localhost:9995/uib/logon
echo
echo
