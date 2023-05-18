package com.qijin.dto;

import cn.afterturn.easypoi.excel.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "excel")
public class templeDTO {

    @ApiModelProperty(value = "MK编码")
    @Excel(name = "MK编码")
    private String mkCode;

    @ApiModelProperty(value = "实付金额")
    @Excel(name = "实付金额")
    private String amount;

}
