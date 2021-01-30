package com.bzzn.stability.dto.es;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author: ls
 * @date: 2021/1/30 15:23
 **/
@Data
@ApiModel("文档新增、更新")
@Accessors(chain = true)
public class UpdateDocument {

    @ApiModelProperty(value = "索引")
    @NotBlank(message = "索性不能为空")
    private String index;

    @ApiModelProperty(value = "文档id")
    @NotBlank(message = "文档id不能为空")
    private String docId;

    @ApiModelProperty(value = "文档内容")
    @NotNull(message = "内容不能为空")
    private JSONObject doc;
}
