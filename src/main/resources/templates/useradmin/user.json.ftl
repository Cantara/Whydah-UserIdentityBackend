<#-- used by UserAggregateResource and UsersResource -->
{
<#-- <#assign identity=user.identity>
"identity": <#include "useridentity.json.ftl"/>,
-->
<#assign identity=user.identity>
"uid":"${identity.uid?js_string}",
"username":"${identity.username?js_string}",
"firstName":"${identity.firstName?js_string}",
"lastName":"${identity.lastName?js_string}",
"email":"${identity.email!?js_string}",
"cellPhone":"${identity.cellPhone!?js_string}",
"personRef":"${identity.personRef!?js_string}",
"roles":[
<#list user.roles as rolle>
   <#include "role.json.ftl"/><#if rolle_has_next>,</#if>
</#list>
],
"uri":"${userbaseurl+"useradmin/users/"+identity.uid+"/"?js_string}"
}