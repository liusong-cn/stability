package com.bzzn.stability.dto.es;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * @author: ls
 * @date: 2021/2/7 11:17
 **/
@Data
@ApiModel
public class IndexCreateDTO {

    @NotBlank(message = "索引名称不能为空")
    @ApiModelProperty(value = "索引名称", required = true)
    private String indexName;

    @ApiModelProperty(value = "分片数量")
    @Min(value = 1, message = "分片数量大于0")
    private int shards = 1;

    @ApiModelProperty(value = "备份数量")
    @Min(value = 1, message = "备份数量大于0")
    private int replicas = 1;
}
