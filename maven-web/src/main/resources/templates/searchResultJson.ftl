{
"success": true,
"results": [
        <#list results as item >
                {
                "name": "<@resultsName item/>",
                "value": "${item.item.groupId}:${item.item.artifactId}",
                "text":  "${item.item.groupId}:${item.item.artifactId}"
                }
            <#if item_has_next>,</#if>
        </#list>
]
}
<#macro resultsName  item><span   class='text' >${item.highlightText?split(":")[0]}  <#if item.item.describe?? > <i class=' icon grey link file alternate outline' onclick='clickDocButton=true;' ></i> </#if> </span>  <span class='lastTime description'> ${item.item.lastModified?number_to_datetime?string("yyyy-MM-dd")!}</span>   <span class='group description'>${item.highlightText?split(":")[1]}</span></#macro>