package com.bzzn.stability.controller.es;

import com.alibaba.fastjson.JSONObject;
import com.bzzn.stability.dto.es.DocumentPageInfo;
import com.bzzn.stability.dto.es.IndexCreateDTO;
import com.bzzn.stability.dto.es.UpdateDocument;
import com.bzzn.stability.service.es.ElasticSearchService;
import com.bzzn.stability.utils.common.Result;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

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
    @ApiOperation("分页索引")
    public Result getIndices(@RequestParam(defaultValue = "1") int currPage,
                             @RequestParam(defaultValue = "10") int pageSize) throws IOException {
        return Result.success(elasticSearchService.queryIndicesWithPage(currPage, pageSize));
    }

    @GetMapping("/getIndexDetail")
    @ApiOperation("索引详情")
    public Result getIndexDetail(@RequestParam String indexName) throws IOException {
        return Result.success(elasticSearchService.getJSONRes(indexName));
    }

    @GetMapping("/getIndexMapping")
    @ApiOperation("索引映射")
    public Result getIndexMapping(@RequestParam @NotBlank(message = "索引名称不能为空") String indexName){
        return Result.success(elasticSearchService.getMapping(indexName));
    }

    @GetMapping("/queryDocument")
    @ApiOperation("索引文档")
    public Result<List<JSONObject>> queryDocument(@RequestParam @NotBlank(message = "索引名称不为空") String indexName,
                                                  @RequestParam @NotBlank(message = "查询值不为空") String searchVal) throws IOException {
        return Result.success(elasticSearchService.queryDocuments(indexName, searchVal));
    }

    //查询有问题
    @GetMapping("/getIndexDocumentPage")
    public Result<DocumentPageInfo> getIndexDocumentPage(@RequestParam(defaultValue = "1") int currPage,
                                                         @RequestParam(defaultValue = "10") int pageSize,
                                                         @NotBlank(message = "索引不能为空") @RequestParam String index) throws IOException {
        return Result.success(elasticSearchService.queryDocPage(currPage, pageSize, index));
    }

    @PutMapping("/createIndex")
    @ApiOperation("新增索引")
    public Result createIndex(@RequestBody IndexCreateDTO indexCreateDTO) throws IOException {
        return Result.success(elasticSearchService.createIndex(indexCreateDTO));
    }

    @ApiOperation("删除索引")
    @DeleteMapping("/deleteIndex")
    public Result deleteIndex(@RequestParam @NotBlank(message = "索引名不能为空") String indexName) throws IOException {
        return Result.success(elasticSearchService.deleteIndex(indexName));
    }

    @ApiOperation("新建文档")
    @PutMapping("/createDocument")
    public Result createDocument(@RequestBody @Valid UpdateDocument document) throws IOException {
        return Result.success(elasticSearchService.createDocument(document));
    }

    @ApiOperation("/更新文档,增量或全量替换")
    @PostMapping("/updateDocument")
    public Result updateDocument(@Valid @RequestBody UpdateDocument document) throws IOException {
        return Result.success(elasticSearchService.updateDocument(document));
    }

    @DeleteMapping("/deleteDocument")
    @ApiOperation("删除文档")
    public Result deleteDocument(@RequestParam String indexName, @RequestParam String docId) throws IOException {
        return Result.success(elasticSearchService.deleteDocument(indexName, docId));
    }

    @GetMapping("/getDocumentDetail")
    @ApiOperation("文档详情")
    public Result getDocumentDetail(@NotBlank(message = "索引不能为空")@RequestParam String indexName,
                                    @NotBlank(message = "文档id不能为空")@RequestParam String docId) throws IOException {
        return Result.success(elasticSearchService.getDocument(indexName, docId));
    }

    @PostMapping("/uploadFile")
    @ApiOperation("文档上传")
    public Result uploadFile(@RequestParam @NotBlank(message = "索引不能为空") String indexName,
                             @RequestParam @NotNull(message = "文件不能为空") MultipartFile file){

        elasticSearchService.uploadFile(indexName, file);
        return Result.success(null);
    }

    @PostMapping("/bulkDocuments")
    @ApiOperation("文档形式添加Documents")
    public Result bulkDocuments(@RequestParam @NotBlank(message = "索引名称不为空") String indexName,
                                @RequestParam @NotNull(message = "文件不能为空") MultipartFile file) throws IOException {
        elasticSearchService.bulkDocuments(file, indexName);
        return Result.success(null);
    }



}
