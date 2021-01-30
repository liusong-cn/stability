package com.bzzn.stability.dto.es;

import com.bzzn.stability.utils.PageInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author: ls
 * @date: 2021/1/30 16:30
 **/
@Data
public class DocumentPageInfo extends PageInfo {

    @ApiModelProperty(value = "字段列表", dataType = "List")
    private List fields;

    public DocumentPageInfo(Integer currPage, Integer pageSize) {
        super(currPage, pageSize);
    }
}
