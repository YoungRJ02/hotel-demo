package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HotelSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handle(response);
    }

    @Test
    void testMatch() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handle(response);
    }

    @Test
    void testBool() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("city", "北京"));
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQueryBuilder);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handle(response);
    }

    @Test
    void testHighlight() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handle(response);
    }

    @Test
    void testPageAndSort() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().sort("price", SortOrder.ASC);
        request.source().from(0).size(5);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handle(response);
    }

    @Test
    void testAgg() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().size(0);
        request.source().aggregation(AggregationBuilders
                .terms("brand")
                .field("brand")
                .size(10));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms brand = aggregations.get("brand");
        List<? extends Terms.Bucket> buckets = brand.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println(keyAsString);
        }
    }

    @Test
    void testSuggest() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix("s")
                        .skipDuplicates(true)
                        .size(10)
        ));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Suggest suggest = response.getSuggest();
        CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");
        for (CompletionSuggestion.Entry.Option option : suggestion.getOptions()) {
            String text = option.getText().toString();
            System.out.println(text);
        }
    }


    private static void handle(SearchResponse response) {
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits().value;
        System.out.println("共搜索到" + total);
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)){
                HighlightField field = highlightFields.get("name");
                if (field != null){
                    String name = field.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }

            System.out.println("hotel = " + hotelDoc);
        }
    }

    @BeforeEach
    void setUp(){
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.88.150:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
