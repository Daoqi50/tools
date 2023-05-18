package com.qijin.utils;

import cn.hutool.core.io.resource.ClassPathResource;
import org.apache.poi.util.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 模板资源下载
 */
public class ExcelTemplateUtils {

    private static final String PATH = "template/";

    /**
     * 下载excel模板
     * @param response
     * @param fileName
     * @throws UnsupportedEncodingException
     */
    public static void downloadByFileName(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        setDownloadResponse(response, fileName);
        ClassPathResource resource = new ClassPathResource(PATH + fileName);
        try (InputStream in = resource.getStream(); OutputStream os = response.getOutputStream()){
            IOUtils.copy( in, os);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件读取异常");
        }
    }

    public static void setDownloadResponse(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.reset();
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
    }
}
