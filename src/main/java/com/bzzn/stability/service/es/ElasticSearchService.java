package com.bzzn.stability.service.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bzzn.stability.dto.es.DocumentPageInfo;
import com.bzzn.stability.dto.es.UpdateDocument;
import com.bzzn.stability.utils.PageInfo;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.get.GetResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    RestClientBuilder getRestClientBuilder(){
        String[] hostArray = hosts.split(",");
        List<HttpHost> hostList = new ArrayList<>();
        for (String host : hostArray) {
            String[] hostSplit = host.split(":");
            String addr = hostSplit[0];
            int port = 9200;
            if(hostSplit.length > 1)
                port = Integer.valueOf(hostSplit[1]);
            hostList.add(new HttpHost(addr, port, "http"));
        }
        return RestClient.builder(hostList.stream().toArray(HttpHost[]::new));
    }

    public RestClient getClient(){
        return getRestClientBuilder().build();
    }

    public RestHighLevelClient getHighLevelClient(){
        return new RestHighLevelClient(getRestClientBuilder());
    }

    public PageInfo<List> queryIndicesWithPage(int currPage, int pageSize) throws IOException {
        try(RestClient restClient = getClient();){
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

            if(resultWithDotList != null)
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

    public String updateDocument(UpdateDocument document) throws IOException {
        try (RestHighLevelClient highLevelClient = getHighLevelClient()){
            UpdateRequest request = new UpdateRequest(document.getIndex(), document.getDocId()).doc(document.getDoc());
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            UpdateResponse response = highLevelClient.update(request, RequestOptions.DEFAULT);
            return response.getResult().toString();
        }
    }

    public DocumentPageInfo queryDocPage(int currPage, int pageSize, String index) throws IOException {
        DocumentPageInfo pageInfo = new DocumentPageInfo(currPage, pageSize);
        List fields = new ArrayList();
        JSONObject mappings = queryIndexMapping(index);
        Object o = mappings.get("properties");
        if(o != null){
            Map fieldMap = JSON.parseObject(o.toString(), Map.class);
            fieldMap.forEach( (k, v) ->{
                fields.add(k);
            });
        }

        List<Map> pageList = new ArrayList<>();
        if(!fields.isEmpty()){
            try (RestClient restClient = getClient()){
                Request request = new Request("get", "/_search");
                JSONObject queryBody = new JSONObject();
                queryBody.put("from", (currPage -1)*pageSize );
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

                pageList.forEach( item ->{
                    Map sources = (Map) item.get("_source");
                    if(sources.size() != fields.size())
                        fields.forEach( f -> sources.put(f, sources.get(f)));
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




    private int[] calculatePage(Collection collection, int curPage, int pageSize){
        if(collection == null || collection.isEmpty())
            throw new RuntimeException("待分页数据不能为空");
        int startIndex = (curPage -1)*pageSize;
        int endIndex = startIndex + pageSize;
        int size = collection.size();
        if(pageSize >= size)
            endIndex = startIndex + size;
        int[] merge = new int[]{startIndex, endIndex, size};
        return merge;
    }

    /**
     * 通用get请求
     * @param requestStr
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T getRes(String requestStr, Class<T> type) throws IOException {
        try(RestClient restClient = getClient()){
            Request request = new Request("get", requestStr);
            Response response = restClient.performRequest(request);
            T r = JSON.parseObject(EntityUtils.toString(response.getEntity()), type);
            return r;
        }
    }

    /**
     * 通用get返回json
     * @param requestStr
     * @return
     * @throws IOException
     */
    public JSONObject getJSONRes(String requestStr) throws IOException {
        try(RestClient restClient = getClient()){
            Request request = new Request("get", requestStr);
            Response response = restClient.performRequest(request);
            JSONObject jsonObject = JSON.parseObject(EntityUtils.toString(response.getEntity()));
            return jsonObject;
        }
    }
}
