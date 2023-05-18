package com.qijin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itextpdf.kernel.geom.PageSize;
import com.qijin.utils.HtmlToPdfUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;

@Service
public class ExportPdfService {

//    private final static String PATH = "static/font/SongTi.ttf";

    public void exportPdf(MultipartFile file, OutputStream outputStream, String paperSize) throws Exception {

        String waterMarkText =  "";
        InputStream inputStream = file.getInputStream();
        // 默认A4
        PageSize pageSize = PageSize.A4;
        if (StrUtil.isNotBlank(paperSize) && "A5".equals(paperSize)) {
            pageSize = new PageSize(595.0F, 420.0F);
        }
        //微软雅黑在windows系统里的位置如下，linux系统直接拷贝该文件放在linux目录下即可
        HtmlToPdfUtils.convertToPdf(inputStream, waterMarkText, "", outputStream, pageSize);

    }

}
