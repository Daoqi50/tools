package com.qijin.utils;


import cn.hutool.core.util.StrUtil;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.font.FontProvider;
import com.qijin.handler.PageEventHandler;
import com.qijin.handler.WaterMarkEventHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HtmlToPdfUtils {

    /**
     * html转pdf
     *
     * @param inputStream  输入流
     * @param waterMark    水印
     * @param fontPath     字体路径，ttc后缀的字体需要添加<b>,0<b/>
     * @param outputStream 输出流
     */
    public static void convertToPdf(InputStream inputStream, String waterMark, String fontPath, OutputStream outputStream, PageSize pageSize) throws IOException {

        PdfWriter pdfWriter = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        // 设置为A4大小
        pdfDocument.setDefaultPageSize(pageSize);
        // 添加水印
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, new WaterMarkEventHandler(waterMark));
        // 添加页脚
        PageEventHandler pageEventHandler = new PageEventHandler();
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, pageEventHandler);

        //添加中文字体支持
        ConverterProperties properties = new ConverterProperties();
        FontProvider fontProvider = new FontProvider();

        PdfFont sysFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H", false);
        fontProvider.addFont(sysFont.getFontProgram(), "UniGB-UCS2-H");

        //添加自定义字体，例如微软雅黑
        if (StrUtil.isNotBlank(fontPath)) {
            PdfFont microsoft = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, false);
            fontProvider.addFont(microsoft.getFontProgram(), PdfEncodings.IDENTITY_H);
        }

        properties.setFontProvider(fontProvider);
        // 读取Html文件流，查找出当中的&nbsp;或出现类似的符号空格字符
        inputStream = readInputStream(inputStream);
        HtmlConverter.convertToPdf(inputStream, pdfDocument, properties);
        pdfDocument.close();
    }

    /**
     * 读取HTML 流文件，并查询当中的&nbsp;或类似符号直接替换为空格
     *
     * @param inputStream
     * @return
     */
    private static InputStream readInputStream(InputStream inputStream) {
        // 定义一些特殊字符的正则表达式 如：
        String regExSpecial = "\\&[a-zA-Z]{1,10};";
        try {
            //<1>创建字节数组输出流，用来输出读取到的内容
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //<2>创建缓存大小 1KB
            byte[] buffer = new byte[1024];
            //每次读取到内容的长度
            int len;
            //<3>开始读取输入流中的内容 当等于-1说明没有数据可以读取了
            while ((len = inputStream.read(buffer)) != -1) {
                //把读取到的内容写到输出流中
                baos.write(buffer, 0, len);
            }
            //<4> 把字节数组转换为字符串
            String content = baos.toString();
            // <5>关闭输入流和输出流
            // inputStream.close();
            baos.close();
            Pattern compile = Pattern.compile(regExSpecial, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compile.matcher(content);
            String replaceAll = matcher.replaceAll("");
            //<6>返回结果
            return getStringStream(replaceAll);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("错误信息：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 将一个字符串转化为输入流
     *
     * @param sInputString 字符串
     * @return
     */
    public static InputStream getStringStream(String sInputString) {
        if (sInputString != null && !"".equals(sInputString.trim())) {
            try {
                return new ByteArrayInputStream(sInputString.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
