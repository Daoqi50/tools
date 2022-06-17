package com.qijin.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ZipUtil;
import com.google.common.io.Files;
import com.qijin.utils.RestTemplateUtils;
import com.qijin.utils.fileToMultipartFile;
import com.qijin.vo.DownPdfPictureVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.Map;

@Api(tags = "Biz:IoUtilController", description = "管理功能")
@RestController
@RequestMapping("v1/ioUtil")
public class IoUtilController {


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
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 100);
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

    @PostMapping(value = "/fileToMulFile")
    @ApiOperation("fileToMulFile")
    public void fileToMulFile(@RequestBody DownPdfPictureVO downPdfPictureVO) throws Exception {

        File tempDir = Files.createTempDir();
        String fileName = downPdfPictureVO.getFilename().replace(".", "").replace("*", "x")
                .replace("/", "").replace(":", "")
                .replace("<", "(")
                .replace(">", ")")
                .replace("|", "");
        File file;
        try {
            URI uri2 = UriComponentsBuilder.fromUriString(downPdfPictureVO.getUrl()).build(true).toUri();
            ResponseEntity<byte[]> bytes = RestTemplateUtils.getRestTemplate().exchange(uri2, HttpMethod.GET, null, byte[].class);

            if (bytes.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("pdf")) {
                file = Paths.get(tempDir.getPath(), fileName + ".pdf").toFile();
                Files.write(bytes.getBody(), file);
            } else if (bytes.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM_VALUE) && bytes.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(".zip")) {
                file = Paths.get(tempDir.getPath(), fileName + ".zip").toFile();
                Files.write(bytes.getBody(), file);
            } else {
                throw new RuntimeException("未知文件" + bytes.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE) + bytes.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
            }
        } catch (Exception e) {
            throw new RuntimeException("下载pdf失败", e);
        }
        MultipartFile multipartFile = fileToMultipartFile.returnMultipartFile(file);

    }
}
