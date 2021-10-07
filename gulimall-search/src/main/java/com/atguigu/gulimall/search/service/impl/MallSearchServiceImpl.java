package com.atguigu.gulimall.search.service.impl;

import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author: CyS2020
 * @date: 2021/10/6
 * 描述：实现搜索接口
 */

@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 去es中检索
     * TODO 先编写DSL语句, 放在resources/gulimall_product_search.json
     */
    @Override
    public SearchResult search(SearchParam param) {
        // 1. 使用java代码构建出DSL语句
        SearchRequest request = buildSearchRequest(param);
        SearchResult result = null;
        try {
            // 2. 执行检索请求
            SearchResponse response = client.search(request, GulimallElasticSearchConfig.COMMON_OPTIONS);

            // 3. 分析响应数据封装成需要的格式
            result = buildSearchResult(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 使用java代码构建出DSL语句
     * 完成功能: 模糊匹配, 过滤(按照属性, 分类, 品牌, 价格区间, 库存), 排序, 分页, 高亮功能, 聚合分析
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // 1. 构建bool - query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 1.1 must - 模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2 bool - filter - 按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // 1.2 bool - filter - 按照品牌id
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }

        // 1.2 bool - filter - 按照所有指定的属性
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] split = attrStr.split("_");
                String attrId = split[0];
                String[] attrValues = split[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个必须都得生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // 1.2 bool - filter - 按照库存是否有进行查询
        boolQuery.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));

        // 1.2 bool - filter - 按照价格区间
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] split = param.getSkuPrice().split("_");
            if (param.getSkuPrice().startsWith("_")) {
                rangeQuery.lte(split[1]);
            } else if (param.getSkuPrice().endsWith("_")) {
                rangeQuery.gte(split[0]);
            } else {
                rangeQuery.gte(split[0]).lte(split[1]);
            }
            boolQuery.filter(rangeQuery);
        }

        sourceBuilder.query(boolQuery);

        // 2.1 排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] split = sort.split("_");
            sourceBuilder.sort(split[0], SortOrder.fromString(split[1]));
        }

        // 2.2 分页
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        // 3.1 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌聚合的子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brandAgg);

        // 3.2 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        // 分类聚合的子聚合
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);

        // 3.3 属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 属性聚合的子聚合
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        log.info("成功构建DSL: {}", sourceBuilder.toString());
        return new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
    }

    /**
     * 构建结果数据
     */
    private SearchResult buildSearchResult(SearchResponse response) {

        return null;
    }
}
