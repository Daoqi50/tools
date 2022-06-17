package com.qijin.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value = "DownPdfPictureVO", description = "下载pdf图片")
public class DownPdfPictureVO {
    private String filename;
    private String url;
}
