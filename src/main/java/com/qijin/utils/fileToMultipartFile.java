package com.qijin.utils;

import cn.hutool.http.ContentType;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;

public class fileToMultipartFile {

    public static MultipartFile returnMultipartFile(File file) {
        File tempDir = Files.createTempDir();
        DiskFileItemFactory factory = new DiskFileItemFactory(16, null);
        // 临时文件夹路径
        factory.setRepository(tempDir);
        FileItem item = factory.createItem("file", ContentType.MULTIPART.getValue(), true, file.getName());
        CommonsMultipartFile multipartFile = new CommonsMultipartFile(item);
        return multipartFile;
    }

}
