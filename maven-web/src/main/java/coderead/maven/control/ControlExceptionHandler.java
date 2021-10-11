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
import org.springframework.web.client.HttpClientErrorException;

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
        Map map = new HashMap();
        map.put("path", request.getRequestURI());
        map.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (ex instanceof HttpClientErrorException) {
            HttpClientErrorException clientError= (HttpClientErrorException) ex;
            response.setStatus(clientError.getStatusCode().value());
            map.put("errorCode", response.getStatus());
            map.put("errorMsg", (clientError.getStatusText()));
            logger.error("客户端请求错误 url: {} 。{}", request.getRequestURI() + "?" + request.getQueryString(),ex.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("客户端请求错误",ex);
            }
        }else{
            String traceId = DigestUtil.md5Hex16(UUID.randomUUID().toString());
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            map.put("errorCode", response.getStatus());
            map.put("errorMsg", ex.getClass().getSimpleName() + ":" + ex.getMessage());
            map.put("traceId", traceId);
            logger.error("服务内部错误 url: {} traceId:{}", request.getRequestURI() + "?" + request.getQueryString(),traceId);
            logger.error("traceId:" + traceId, ex);
        }
        return map;
    }

}
