<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<whydahuser>
    <identity>
        <username>${it.identity.username?xml}</username>
        <cellPhone>${it.identity.cellPhone!?xml}</cellPhone>
        <email>${it.identity.email!?xml}</email>
        <firstname>${it.identity.firstName?xml}</firstname>
        <lastname>${it.identity.lastName?xml}</lastname>
        <personRef>${it.identity.personRef!?xml}</personRef>
        <UID>${it.identity.uid?xml}</UID>
    </identity>
    <applications>
<#list it.propsAndRoles as role>
        <application>
            <appId>${role.applicationId?xml}</appId>
            <applicationName>${role.applicationName?xml}</applicationName>
            <orgID>${role.organizationId?xml}</orgID>
            <roleName>${role.roleName?xml}</roleName>
            <roleValue>${role.roleValue!?xml}</roleValue>
        </application>
</#list>
    </applications>
</whydahuser>