package com.bzzn.stability.controller.es;

import com.bzzn.stability.dto.es.DocumentPageInfo;
import com.bzzn.stability.dto.es.UpdateDocument;
import com.bzzn.stability.service.es.ElasticSearchService;
import com.bzzn.stability.utils.common.Result;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;

/**
 * @author: ls
 * @date: 2021/1/30 14:01
 **/
@RestController
@RequestMapping("/es")
@Validated
public class ElasticsearchController {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @GetMapping("/getIndices")
    public Result getIndices(@RequestParam(defaultValue = "1") int currPage,
                             @RequestParam(defaultValue = "10") int pageSize) throws IOException {
        return Result.success(elasticSearchService.queryIndicesWithPage(currPage, pageSize));
    }

    @GetMapping("/getIndexDetail")
    public Result getIndexDetail(@RequestParam String indexName) throws IOException {
        return Result.success(elasticSearchService.getJSONRes(indexName));
    }

    @GetMapping("/getIndexDocumentPage")
    public Result<DocumentPageInfo> getIndexDocumentPage(@RequestParam(defaultValue = "1") int currPage,
                                                         @RequestParam(defaultValue = "10") int pageSize,
                                                         @NotBlank(message = "索引不能为空") @RequestParam String index) throws IOException {
        return Result.success(elasticSearchService.queryDocPage(currPage, pageSize, index));
    }

    @ApiOperation("/新增.更新文档")
    @PostMapping("/updateDocument")
    public Result updateDocument(@Valid @RequestBody UpdateDocument document) throws IOException {
        return Result.success(elasticSearchService.updateDocument(document));
    }
}
