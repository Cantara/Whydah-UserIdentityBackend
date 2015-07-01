{
"rows":"${users?size}",
"result":
[<#list users as identity><#include "useridentity.json.ftl"/><#if identity_has_next>,</#if></#list>]
}