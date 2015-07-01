{"applications" : [
<#list allApps as app>
    {
        "appId" : "${app.applicationId?js_string}",
        "applicationName" : "${app.name?js_string}",
        "hasRoles" : ${myApps?seq_contains(app.applicationId)?string("true", "false")}
    }<#if app_has_next>,</#if>
</#list>
]}