package com.qijin.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PascalNameFilter;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * @Author: czl
 * @Date: 2020/11/18 14:40
 */
public class RestTemplateUtils {

    private final static Logger logger = LoggerFactory.getLogger(RestTemplateUtils.class);

    /**
     * Content_Type - gzip
     */
    public static final MediaType GZIP_MEDIA_TYPE = MediaType.valueOf("application/gzip");

    private static RestTemplate restTemplateForNet;
    private static RestTemplate restTemplate;
    private static HttpComponentsClientHttpRequestFactory httpRequestFactory;

    static {
        httpRequestFactory = getHttpRequestFactory();

        RestTemplate template = new RestTemplate(httpRequestFactory);
        addNetMessageConvert(template);
        addLogInterceptor(template);
        restTemplateForNet = template;

        restTemplate = new RestTemplate(httpRequestFactory);
        addLogInterceptor(restTemplate);
    }

    public static void addLogInterceptor(RestTemplate template) {
        List<ClientHttpRequestInterceptor> interceptors = template.getInterceptors();
        LogClientHttpRequestInterceptor logClientHttpRequestInterceptor = new LogClientHttpRequestInterceptor();
        interceptors.add(logClientHttpRequestInterceptor);
    }

    public static void addResponseInterceptor(RestTemplate template, Consumer<String> action) {
        List<ClientHttpRequestInterceptor> interceptors = template.getInterceptors();
        ResponseHandlerClientHttpRequestInterceptor responseHandlerClientHttpRequestInterceptor = new ResponseHandlerClientHttpRequestInterceptor(action);
        interceptors.add(responseHandlerClientHttpRequestInterceptor);
    }

    public static void addNetMessageConvert(RestTemplate template) {
        List<HttpMessageConverter<?>> messageConverters = template.getMessageConverters();
        messageConverters.removeIf(x -> x.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));
        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
        fastJsonHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        fastJsonHttpMessageConverter.getFastJsonConfig().setSerializeFilters(new PascalNameFilter());
        messageConverters.add(fastJsonHttpMessageConverter);
    }

    /**
     * 添加GZIP的消息转化器
     * @param template
     */
    public static void addGzipMessageConvert(RestTemplate template) {
        template.getMessageConverters().add(new GzipMessageConverter());
    }

    public static HttpComponentsClientHttpRequestFactory getHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(10 * 1000);   //从连接池获取连接时间
        httpRequestFactory.setConnectTimeout(3 * 1000);              //建立连接时间
        httpRequestFactory.setReadTimeout(60 * 1000);                //整个调用的时间
        return httpRequestFactory;
    }

    public static RestTemplate getRestTemplateForNet() {
        return restTemplateForNet;
    }

    public static RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public static class LogClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

            StringBuilder headerStringBuilder = new StringBuilder();
            request.getHeaders().entrySet().forEach(x -> {
                headerStringBuilder.append(x.getKey());
                headerStringBuilder.append(":");
                headerStringBuilder.append(x.getValue());
                headerStringBuilder.append(";");
            });

            long startTime = System.currentTimeMillis();

            ClientHttpResponse response = execution.execute(request, body);

            ClientHttpResponseWrapper clientHttpResponseWrapper = new ClientHttpResponseWrapper(response);

//            if(response.getHeaders().getContentType().getType()){
//
//            }else{
//
//            }
            String responseBody = clientHttpResponseWrapper.getBodyAsString();

            String message = String.format("duration:%s url:%s requestHeaders:%s requestBody:%s responseBody:%s",
                    (System.currentTimeMillis() - startTime) + "ms", request.getURI().toString(), headerStringBuilder.toString(), new String(body), responseBody);
            logger.info(message);

            return clientHttpResponseWrapper;
        }
    }

    public static class ClientHttpResponseWrapper extends AbstractClientHttpResponse {

        ClientHttpResponse clientHttpResponse;

        private final byte[] bytes;

        public ClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
            clientHttpResponse = response;
            bytes = IOUtils.toByteArray(response.getBody());
        }

        public String getBodyAsString() throws UnsupportedEncodingException {
            return new String(bytes, "utf8");
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return clientHttpResponse.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return clientHttpResponse.getStatusText();
        }

        @Override
        public void close() {
            clientHttpResponse.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            InputStream is = new ByteArrayInputStream(bytes);
            if(null != clientHttpResponse.getHeaders().getContentType()
                    && clientHttpResponse.getHeaders().getContentType().equals(GZIP_MEDIA_TYPE)){
                is = new GZIPInputStream(is);
            }

            return is;
        }

        @Override
        public HttpHeaders getHeaders() {
            return clientHttpResponse.getHeaders();
        }
    }



    public static class ResponseHandlerClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

        Consumer<String> action;

        public ResponseHandlerClientHttpRequestInterceptor(Consumer<String> action) {
            this.action = action;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            ClientHttpResponse response = execution.execute(request, body);
            ClientHttpResponseWrapper clientHttpResponseWrapper = new ClientHttpResponseWrapper(response);
            String responseBody = clientHttpResponseWrapper.getBodyAsString();
            action.accept(responseBody);
            return clientHttpResponseWrapper;
        }
    }


    @AllArgsConstructor
    public static class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {

        private final String headerName;

        private final String headerValue;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(headerName, headerValue);
            return execution.execute(request, body);
        }
    }

    @AllArgsConstructor
    public static class BodyDataRequestInterceptor implements ClientHttpRequestInterceptor {

        private final Map<String, Object> data;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            if(MapUtils.isNotEmpty(data)){
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                if(StringUtils.isBlank(bodyStr)){
                    bodyStr = "{}";
                }
                JSONObject object = JSON.parseObject(bodyStr);
                object.putAll(data);
                body = object.toJSONString().getBytes(StandardCharsets.UTF_8);
            }
            return execution.execute(request, body);
        }
    }

    public static class GzipMessageConverter extends MappingJackson2HttpMessageConverter {
        public GzipMessageConverter(){
            List<MediaType> mediaTypes = new ArrayList<>();
            mediaTypes.add(GZIP_MEDIA_TYPE);
            setSupportedMediaTypes(mediaTypes);
        }
    }

    public static class BizResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException{
            return super.hasError(response);
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException{
            ClientHttpResponseWrapper clientHttpResponseWrapper = new ClientHttpResponseWrapper(response);
            if(clientHttpResponseWrapper.getRawStatusCode() != 200){
                throw new RuntimeException("请求接口出错，" + clientHttpResponseWrapper.getStatusText());
            }else{
                super.handleError(response);
            }
        }

    }
}
