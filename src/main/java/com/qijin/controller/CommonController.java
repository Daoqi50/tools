package com.qijin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.google.common.io.Files;
import com.qijin.service.impl.ExportPdfService;
import com.qijin.utils.ExcelExportPlusUtil;
import com.qijin.utils.ExcelImportPlusUtil;
import com.qijin.dto.templeDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "公共接口")
@RequestMapping("/v1/common")
public class CommonController {

    @Autowired
    private ExportPdfService exportPdfService;

    @ApiOperation(value = "匹配 excel字段", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PostMapping(value = "/matchExcelFiled")
    public void matchExcelFiled(@RequestPart(name = "excel") MultipartFile[] excel, HttpServletResponse response, HttpServletRequest request) throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {

        List<templeDTO> templeDTOList123 = null;
        List<templeDTO> templeDTOList456 = null;
        for (MultipartFile multipartFile : excel) {
            List<templeDTO> templeDTOList  = ExcelImportPlusUtil.importExcel(multipartFile.getInputStream(), templeDTO.class);
            List<String> collect = templeDTOList.stream().map(templeDTO::getAmount).filter(StrUtil::isNotBlank).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(collect)) {
                templeDTOList456 = templeDTOList;
            } else {
                templeDTOList123 = templeDTOList;
            }
        }
        Map<String, List<templeDTO>> groupByCode = templeDTOList456.stream().distinct().collect(Collectors.groupingBy(templeDTO::getMkCode));

        templeDTOList123.forEach(e -> {
            if (groupByCode.get(e.getMkCode()) != null) {
                String amount = groupByCode.get(e.getMkCode()).stream().map(templeDTO::getAmount).collect(Collectors.joining(","));
                e.setAmount(amount);
            }
        });
        ExcelExportPlusUtil.export(templeDTOList123, templeDTO.class, request, response);
    }

    @PostMapping("/htmlToPdf")
    @ApiOperation(value = "html转换Pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void htmlToPdf(@RequestPart(name = "htmlFile") MultipartFile htmlFile, String paperSize, HttpServletResponse response) throws Exception {
        try {
            String fileNamePrefix = Objects.requireNonNull(htmlFile.getOriginalFilename()).substring(0, htmlFile.getOriginalFilename().lastIndexOf("."));
            String date = DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);
            String fileName = fileNamePrefix + "_" + date + ".pdf";
            this.resolveResponse(response, fileName);
            this.exportPdfService.exportPdf(htmlFile, response.getOutputStream(), paperSize);
        } catch (Exception e) {
            this.resetResponse(response, e);
        }
    }

    @PostMapping(value = "/convertPic")
    @ApiOperation("pdf转pic")
    public void convertPic(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws Exception {
        File tempDir = Files.createTempDir();
        File pdfFile = Paths.get(tempDir.getPath(), "tmp.pdf").toFile();
        File docxFile = Paths.get(tempDir.getPath(), "tmp.docx").toFile();
        File zipFile = Paths.get(tempDir.getPath(), "file.zip").toFile();
        byte[] bytes = file.getBytes();
        Files.write(bytes, pdfFile);
        if (pdfFile.exists()) {
            File pictureDir = Paths.get(tempDir.getPath(), "picture").toFile();
            pictureDir.mkdirs();

            PDDocument doc = null;
            ByteArrayOutputStream os = null;
            try {
                // 加载解析PDF文件
                doc = PDDocument.load(pdfFile);
                doc.save(docxFile);
                PDFRenderer pdfRenderer = new PDFRenderer(doc);
                PDPageTree pages = doc.getPages();
                int pageCount = pages.getCount();
                for (int i = 0; i < pageCount; i++) {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 200);
                    os = new ByteArrayOutputStream();
                    ImageIO.write(bim, "jpg", os);
                    byte[] dataList = os.toByteArray();
                    //图片路径
                    File newFile = Paths.get(pictureDir.getPath(), i + ".jpg").toFile();
                    Files.write(dataList, newFile);
                }
                ZipUtil.zip(Paths.get(tempDir.getPath(), "file.zip").toFile(), false, pictureDir);

            } catch (Exception e) {
                throw new RuntimeException("转换pdf失败", e);
            } finally {
                if (doc != null) doc.close();
                if (os != null) os.close();
            }

            try(FileInputStream fileInputStream = new FileInputStream(zipFile); final ServletOutputStream outputStream = response.getOutputStream();) {
                response.reset();
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(zipFile.getName(), "utf-8"));
                IoUtil.copy(fileInputStream, outputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileUtils.deleteDirectory(tempDir);
    }

    private void resolveResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/pdf");
        response.setCharacterEncoding("utf-8");
        String encodeFileName = cn.hutool.core.net.URLEncoder.createDefault().encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + encodeFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    private void resetResponse(HttpServletResponse response, Exception exception) throws IOException {
        // 重置response
        response.reset();
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        String stacktrace = ExceptionUtil.stacktraceToString(exception);
        log.error("{}", stacktrace);
        String result = "下载文件失败，" + exception.getMessage();
        response.getWriter().println(result);
    }
}
