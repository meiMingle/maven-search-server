<div class="ui large breadcrumb" style="margin-top: 20px;margin-bottom: 15px;">
    <a class="section" onclick="fillSearchText('${groupId}')">${groupId}</a>
    <div class="divider"> /</div>
    <a class="section" onclick="fillSearchText('${artifactId}')">${artifactId}</a>
    <div class="divider"> /</div>
    <div class="active section">版本列表</div>
</div>

<#if docHtml??>
    <div class="ui ">
        <div class="doc tips">
        </div>
        <div class="doc full" style="display: none">
            ${docHtml}
        </div>
        <div  class="ui    small button unfold" onclick="showDocFull();" tabindex="0">
            <i class="angle double down  icon"></i><span class="text">展开显示更多案例...</span>
        </div>
        <script>
            $(function () {
                let docHtml = $(".doc.full").text();
                let docTips=docHtml.slice(0,Math.min(100,docHtml.length))
                $(".doc.tips").text(docTips+"....");
                // code 渲染

                $(".doc.full pre>code").each(function(){
                    hljs.highlightBlock(this);
                })
                if (clickDocButton) {
                    showDocFull();
                    clickDocButton=false;
                }
            });
        </script>
    </div>
   <#-- <a class="" onclick="showDocFull();">
        展开更多示例...
        <i class="angle double down  icon"></i>
    </a>-->

</#if>


<table class="ui version selectable table">
  <#--  <thead>

    </thead>-->
    <tbody>
    <tr>
        <td class="ten wide">版本</td>
        <td class="right aligned">下载</td>
        <td class="right aligned">引用次数</td>
        <td class="right aligned">发布时间</td>
    </tr>
    <#list items as item>
    <tr onclick="doFold($(this))">
        <td>${item.version}</td>
        <td class="right aligned">
            <i class="download icon" data-url="${item.groupId}/${item.artifactId}/${item.version}" style="z-index: 1000;color: darkgray;"></i>
        </td>
        <td class="right aligned">${item.attributes.versionCount}</td>
        <td class="right aligned"> ${item.lastModified?number_to_datetime?string("yyyy-MM-dd")!}</td>
    </tr>
    <tr class="content" style="display: none">
        <td colspan="3" style="padding: 0px;">
            <div class="ui red attached segment">
                <div class="ui  top attached tabular pointing secondary menu">
                    <a class="item active" data-tab="${item.version}-maven">maven</a>
                    <a class="item" data-tab="${item.version}-gradle">gradle</a>
                </div>
                <div class="ui bottom attached tab segment active" data-tab="${item.version}-maven">
            <textarea rows="6" style="width: 100%;"
                      onfocus="areaOnfocus(this,'${item.groupId}','${item.artifactId}','${item.version}');">
              <dependency>
                    <groupId>${item.groupId}</groupId>
                    <artifactId>${item.artifactId}</artifactId>
                    <version>${item.version}</version>
                </dependency></textarea>
                </div>
                <div class="ui bottom attached tab segment" data-tab="${item.version}-gradle">
                <textarea rows="6" style="width: 100%"
                          onfocus="areaOnfocus(this,'${item.groupId}','${item.artifactId}','${item.version}');">
                    implementation '${item.groupId}:${item.artifactId}:${item.version}'</textarea>
                </div>
            </div>
        </td>
    </tr>
    </#list>
    <#if items?size==0>
        <div class="error message">没有找到任何记录</div>
    </#if>
    </tbody>
</table>

<style>

</style>
<script>
    $(function () {
        initVersionUI();
        // download icon
        $('.download,.icon').click(function (event) {
            console.log(event);
            if ( event && event.stopPropagation )
                event.stopPropagation();        //因此它支持W3C的stopPropagation()方法
            else
                window.event.cancelBubble = true;        //否则，我们需要使用IE的方式来取消事件冒泡


            //https://repo1.maven.org/maven2/org/jsoup/jsoup/1.14.2/jsoup-1.14.2.jar
            //org.apache.zookeeper/zookeeper/3.7.0
            var attr = $(this).attr("data-url");
            console.log(attr);
            var data=attr.split('/');
            var url ="https://repo1.maven.org/maven2/";
            url+=data[0].replaceAll('.','/')+'/';
            url+=data[1];
            url+='/';
            url+=data[2]+'/'+data[1]+'-'+data[2]+'.jar';
            console.log(url);
            window.location=url;

        });
    })

    function showDocFull() {
        $(".doc.full").toggle();
        $(".doc.tips").toggle();
        if ($(".doc.full").is(":hidden")) {
            // 是否可空
            $(".button.unfold span.text").text("展开显示更多案例...")
        }else {
            $(".button.unfold span.text").text("收起")
        }
    }


    function download(e) {
          //如果提供了事件对象，则这是一个非IE浏览器
        if ( e && e.stopPropagation )
            e.stopPropagation();        //因此它支持W3C的stopPropagation()方法
        else
            window.event.cancelBubble = true;        //否则，我们需要使用IE的方式来取消事件冒泡
    }
</script>