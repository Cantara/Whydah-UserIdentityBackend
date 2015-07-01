<html>
<head><title>Logon ok</title></head>
<body>
<b>User info:</b><br>
<table style="border: 1px solid black;" border="1">
    <tr>
        <th>Username</th>
        <td>${it.identity.username}</td>
    </tr>
    <tr>
        <th>FirstName</th>
        <td>${it.identity.firstName}</td>
    </tr>
    <tr>
        <th>LastName</th>
        <td>${it.identity.lastName}</td>
    </tr>
    <tr>
        <th>Cell phone</th>
        <td>${it.identity.cellPhone!}</td>
    </tr>
    <tr>
        <th>Email</th>
        <td>${it.identity.email!}</td>
    </tr>
    <tr>
        <th>Personref</th>
        <td>${it.identity.personRef!}</td>
    </tr>
    <tr>
        <th>Uid</th>
        <td>${it.identity.uid}</td>
    </tr>
</table>
<br>
<b>Roles:</b><br>
<table style="border: 1px solid black;" border="1">
<tr>
    <th>Application</th>
    <th>Organization</th>
    <th>Role</th>
    <th>Role value</th>
<#list it.propsAndRoles as rolle>
    <tr>
        <td>${rolle.applicationName!} (${rolle.applicationId!})</td>
        <td>${rolle.organizationName!} (${rolle.organizationId!})</td>
        <td>${rolle.roleName}</td>
        <td>${rolle.roleValue!}</td>
    </tr>
</#list>
</table>
</body>
</html>