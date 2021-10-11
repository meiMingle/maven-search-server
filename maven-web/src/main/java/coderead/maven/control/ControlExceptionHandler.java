package coderead.maven.control;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import cn.hutool.crypto.digest.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author 鲁班大叔
 * @date 2021
 */
@ControllerAdvice
public class ControlExceptionHandler {
    static final Logger logger = LoggerFactory.getLogger(ControlExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map handlerException(Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String traceId = DigestUtil.md5Hex16(UUID.randomUUID().toString());
        logger.error("api 内部异常 traceId:{},url: {}", traceId, request.getRequestURI() + "?" + request.getQueryString());
        logger.error("traceId:" + traceId, ex);
        response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
        Map map = new HashMap();
        map.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        map.put("path", request.getRequestURI());
        map.put("errorCode", "0001");
        map.put("errorMsg", ex.getClass().getSimpleName() + ":" + ex.getMessage());
        map.put("traceId", traceId);
        return map;
    }
}
