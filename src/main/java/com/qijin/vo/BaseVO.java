package com.qijin.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhuqijin
 * @Date 09:25 2022/12/7
 */
@Data
@Accessors(chain = true)
public class BaseVO implements Serializable {

    /**
     * id
     * */
    @ApiModelProperty("标识")
    private String id;

    /**
     * 删除标记
     * */
    @JsonIgnore
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;
    /**
     * 创建时间
     * */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    /**
     * 创建人
     * */
    @ApiModelProperty(value = "创建人id")
    private String createBy;
    /**
     * 修改时间
     * */
    @ApiModelProperty(value = "修改时间")
    private Date updateTime;
    /**
     * 修改人
     * */
    @ApiModelProperty(value = "修改人id")
    private String updateBy;


}
