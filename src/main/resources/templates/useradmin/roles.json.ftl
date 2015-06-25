<#-- Not in use? -->
{ "propsAndRoles" : [
<#list roller as rolle>
<#include "role.json.ftl"/><#if rolle_has_next>,</#if>
</#list>
]}