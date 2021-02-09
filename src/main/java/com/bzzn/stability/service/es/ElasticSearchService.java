package com.bzzn.stability.service.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.bzzn.stability.dto.es.DocumentPageInfo;
import com.bzzn.stability.dto.es.IndexCreateDTO;
import com.bzzn.stability.dto.es.UpdateDocument;
import com.bzzn.stability.utils.PageInfo;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: ls
 * @date: 2021/1/30 10:55
 **/
@Service
public class ElasticSearchService {

    @Value("${elasticsearch.hosts}")
    String hosts;
    private JSONArray resMappingArray;

    RestClientBuilder getRestClientBuilder() {
        String[] hostArray = hosts.split(",");
        List<HttpHost> hostList = new ArrayList<>();
        for (String host : hostArray) {
            String[] hostSplit = host.split(":");
            String addr = hostSplit[0];
            int port = 9200;
            if (hostSplit.length > 1)
                port = Integer.valueOf(hostSplit[1]);
            hostList.add(new HttpHost(addr, port, "http"));
        }
        return RestClient.builder(hostList.stream().toArray(HttpHost[]::new));
    }

    public RestClient getClient() {
        return getRestClientBuilder().build();
    }

    public RestHighLevelClient getHighLevelClient() {
        return new RestHighLevelClient(getRestClientBuilder());
    }

    public PageInfo<List> queryIndicesWithPage(int currPage, int pageSize) throws IOException {
        try (RestClient restClient = getClient();) {
            Map<String, String> params = new HashMap<>();
            params.put("format", "json");
            params.put("pretty", "true");
            params.put("h", "status,index,docs.count,store.size,creation.date.string");
            params.put("s", "cds:desc");
            Request request = new Request("get", "_cat/indices");
            request.addParameters(params);
            Response response = restClient.performRequest(request);
            List resultList = JSON.parseObject(EntityUtils.toString(response.getEntity()), List.class);

            Request requestWithDot = new Request("get", "_cat/indices/.*");
            Response responseWithDot = restClient.performRequest(requestWithDot);
            List resultWithDotList = JSON.parseObject(EntityUtils.toString(responseWithDot.getEntity()), List.class);

            if (resultWithDotList != null)
                resultList.removeAll(resultWithDotList);

            int[] page = calculatePage(resultList, currPage, pageSize);
            PageInfo pageInfo = new PageInfo(currPage, pageSize);
            pageInfo.setTotalCount(page[2]);
            List pageList = new ArrayList();
            pageList = resultList.subList(page[0], page[1]);
            pageInfo.setList(pageList);
            return pageInfo;
        }
    }

    public String createIndex(IndexCreateDTO dto) throws IOException {
        try (RestClient client = getClient()) {
            Request request = new Request("put", dto.getIndexName());
            JSONObject j = new JSONObject();
            j.put("number_of_shards", dto.getShards());
            j.put("number_of_replicas", dto.getReplicas());
            JSONObject settings = new JSONObject();
            settings.put("settings", j);
            NStringEntity entity = new NStringEntity(settings.toJSONString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            Response response = client.performRequest(request);
            return EntityUtils.toString(response.getEntity());
        }
    }

    public String deleteIndex(String indexName) throws IOException {
        try (RestClient client = getClient()) {
            Request request = new Request("delete", indexName);
            Response response = client.performRequest(request);
            return EntityUtils.toString(response.getEntity());
        }
    }

    public String createDocument(UpdateDocument document) throws IOException {
        try (RestClient client = getClient()) {
            String indexName = document.getIndex();
            String docId = document.getDocId();
            String endPoint = String.format("/%s/_create/%s", indexName, docId);
            Request request = new Request("put", endPoint);
            JSONObject doc = document.getDoc();
            NStringEntity entity = new NStringEntity(doc.toJSONString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            Response response = client.performRequest(request);
            return EntityUtils.toString(response.getEntity());
        }
    }


    public String updateDocument(UpdateDocument document) throws IOException {
        try (RestHighLevelClient highLevelClient = getHighLevelClient()) {
            UpdateRequest request = new UpdateRequest(document.getIndex(), document.getDocId()).doc(document.getDoc());
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            UpdateResponse response = highLevelClient.update(request, RequestOptions.DEFAULT);
            return response.getResult().toString();
        }
    }

    public String deleteDocument(String indexName, String docId) throws IOException {
        try (RestHighLevelClient highLevelClient = getHighLevelClient()) {
            DeleteRequest request = new DeleteRequest(indexName, docId);
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            DeleteResponse response = highLevelClient.delete(request, RequestOptions.DEFAULT);
            return response.getResult().toString();
        }
    }

    public String getDocument(String indexName, String docId) throws IOException {
        String requstStr = String.format("/%s/_source/%s", indexName, docId);
        return getRes(requstStr, String.class);
    }


    public DocumentPageInfo queryDocPage(int currPage, int pageSize, String index) throws IOException {
        DocumentPageInfo pageInfo = new DocumentPageInfo(currPage, pageSize);
        List fields = new ArrayList();
        JSONObject mappings = queryIndexMapping(index);
        Object o = mappings.get("properties");
        if (o != null) {
            Map fieldMap = JSON.parseObject(o.toString(), Map.class);
            fieldMap.forEach((k, v) -> {
                fields.add(k);
            });
        }

        List<Map> pageList = new ArrayList<>();
        if (!fields.isEmpty()) {
            try (RestClient restClient = getClient()) {
                Request request = new Request("get", "/_search");
                JSONObject queryBody = new JSONObject();
                queryBody.put("from", (currPage - 1) * pageSize);
                queryBody.put("size", pageSize);
                JSONObject orderField = new JSONObject();
                orderField.put("order", "asc");
                JSONObject idField = new JSONObject();
                idField.put("_id", orderField);
                queryBody.put("sort", idField);
                NStringEntity entity = new NStringEntity(queryBody.toJSONString(), ContentType.APPLICATION_JSON);
                request.setEntity(entity);

                Response response = restClient.performRequest(request);
                JSONObject jsonResult = JSON.parseObject(EntityUtils.toString(response.getEntity()));
                JSONObject firstHits = (JSONObject) jsonResult.get("hits");
                pageList = JSON.parseObject(firstHits.get("hits").toString(), List.class);
                JSONObject total = (JSONObject) firstHits.get("total");
                Integer totalCount = total.getInteger("value");

                pageList.forEach(item -> {
                    Map sources = (Map) item.get("_source");
                    if (sources.size() != fields.size())
                        fields.forEach(f -> sources.put(f, sources.get(f)));
                });
                pageInfo.setTotalCount(totalCount);
            }
        }

        pageInfo.setList(pageList);
        pageInfo.setFields(fields);
        return pageInfo;

    }

    public JSONObject queryIndexMapping(String index) throws IOException {
        JSONObject jsonObject = getJSONRes("/_mapping");
        JSONObject indexMapping = (JSONObject) jsonObject.get(index);
        return (JSONObject) indexMapping.get("mappings");
    }

    /**
     * 模板下载，未完成
     *
     * @param response
     * @param indexName
     */
    public void dowloadTemplate(HttpServletResponse response, String indexName) {
        String requestStr = String.format("/%s/_mapping", indexName);
        try {
            JSONObject j = getRes(requestStr, JSONObject.class);
            JSONObject indexMapping = j.getJSONObject(indexName);
            JSONObject properties = indexMapping.getJSONObject("properties");
            Map<String, String> fieldsMap = new HashMap<>();
            properties.entrySet().forEach(entry -> {
                String value = (String) ((Map) entry.getValue()).get("type");
                fieldsMap.put(entry.getKey(), value);
            });
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(indexName, "utf-8") + ".json");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            ServletOutputStream out = response.getOutputStream();
            BufferedOutputStream buff = new BufferedOutputStream(out);
            buff.write(JSON.toJSONString(fieldsMap, SerializerFeature.WriteMapNullValue).getBytes("utf-8"));
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ;

    public void uploadFile(String indexName, MultipartFile file) {
        String[] fileName = file.getOriginalFilename().split("\\.");
        String fileType = fileName[fileName.length - 1];
        if (!fileType.equalsIgnoreCase("json"))
            throw new RuntimeException("文档格式不匹配");
        InputStreamReader reader = null;
        BufferedReader breader = null;
        try {
            reader = new InputStreamReader(file.getInputStream(), "utf-8");
            breader = new BufferedReader(reader);
            String inputStr;
            StringBuffer buffer = new StringBuffer();
            while ((inputStr = breader.readLine()) != null) {
                buffer.append(inputStr);
            }
            System.out.println(buffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(breader);
            IOUtils.closeQuietly(reader);
        }
    }

    public String getMapping(String indexName) {
        return JSON.toJSONString(getMappingJSONArray(indexName));
    }

    public List<JSONObject> queryDocuments(String indexName, String searchVal) throws IOException {
        List<JSONObject> res = new ArrayList<>();
        try (final RestClient restClient = getClient()){
            Request request = new Request("post", indexName + "/_search");
            JSONArray mappings = getMappingJSONArray(indexName);
            if(mappings.size() == 0)
                throw  new RuntimeException("缺少text类型字段");
            JSONArray matchs = new JSONArray();
            mappings.toJavaList(JSONObject.class).forEach(j ->{
                if(j.getString("type").equalsIgnoreCase("text")){
                    final JSONObject keyValue = new JSONObject();
                    keyValue.put(j.getString("field"), searchVal);
                    final JSONObject match = new JSONObject();
                    match.put("match", keyValue);
                    matchs.add(match);
                }
            });
            JSONObject should = new JSONObject();
            should.put("should", matchs);
            JSONObject bool = new JSONObject();
            bool.put("bool", should);
            JSONObject query = new JSONObject();
            query.put("query", bool);
            NStringEntity entity = new NStringEntity(JSON.toJSONString(query), ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            Response response = restClient.performRequest(request);

            JSONObject hits = JSON.parseObject(EntityUtils.toString(response.getEntity())).getJSONObject("hits");
            JSONArray realHits = hits.getJSONArray("hits");
            for (int i = 0; i < realHits.size(); i++) {
                JSONObject realHit = realHits.getJSONObject(i);
                JSONObject source = realHit.getJSONObject("_source");
                res.add(source);
            }
        }
        return res;
    }

    /**
     * 字段映射
     * @param indexName
     * @return
     */
    private JSONArray getMappingJSONArray(String indexName){
        JSONArray resMappingArray = new JSONArray();
        try {
            final JSONObject res = getRes(indexName, JSONObject.class);
            JSONObject mapping = res.getJSONObject(indexName).getJSONObject("mappings");
            mapping.getJSONObject("properties").entrySet().forEach(entry -> {
                String key = entry.getKey();
                final JSONObject valueJSON = (JSONObject) entry.getValue();
                String value = valueJSON.getString("type");
                final JSONObject mappingJson = new JSONObject();
                mappingJson.put("field", key);
                mappingJson.put("type", value);
                resMappingArray.add(mappingJson);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resMappingArray;
    }


    private int[] calculatePage(Collection collection, int curPage, int pageSize) {
        if (collection == null || collection.isEmpty())
            throw new RuntimeException("待分页数据不能为空");
        int startIndex = (curPage - 1) * pageSize;
        int endIndex = startIndex + pageSize;
        int size = collection.size();
        if (startIndex > size - 1) {
            startIndex = endIndex = Integer.MAX_VALUE;
        } else if (startIndex + pageSize >= size) {
            endIndex = size;
        }
        int[] merge = new int[]{startIndex, endIndex, size};
        return merge;
    }

    /**
     * 通用get请求
     *
     * @param requestStr
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T getRes(String requestStr, Class<T> type) throws IOException {
        try (RestClient restClient = getClient()) {
            Request request = new Request("get", requestStr);
            Response response = restClient.performRequest(request);
            T r = JSON.parseObject(EntityUtils.toString(response.getEntity()), type);
            return r;
        }
    }

    /**
     * 通用get返回json
     *
     * @param requestStr
     * @return
     * @throws IOException
     */
    public JSONObject getJSONRes(String requestStr) throws IOException {
        try (RestClient restClient = getClient()) {
            Request request = new Request("get", requestStr);
            Response response = restClient.performRequest(request);
            JSONObject jsonObject = JSON.parseObject(EntityUtils.toString(response.getEntity()));
            return jsonObject;
        }
    }
}
