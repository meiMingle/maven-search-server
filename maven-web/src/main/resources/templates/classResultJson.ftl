{
"success": true,
"results": [
        <#list results as item >
                {
                "name": "<span class='text title'>${item.fullClassName?split(" ")[0]} </span> <span class='package description'> ${item.fullClassName?split(" ")[1]}</span> <span class='lastTime description'> ${item.versionModify?string("yyyy-MM-dd")}</span>  <span class='group description'>${item.artifact}</span>   ",
                "value": "${item.artifact}",
                "text":  "${item.artifact}"
                }
            <#if item_has_next>,</#if>
        </#list>
]
}