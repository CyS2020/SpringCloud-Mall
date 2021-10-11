package com.atguigu.gulimall.search.service.impl;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private Gson gson = new Gson();

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
            result = buildSearchResult(response, param);
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
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));
        }

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
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1. 返回所有查询到的商品
        List<SkuEsModel> esModels = new ArrayList<>();
        SearchHits hits = response.getHits();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = gson.fromJson(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLightStr = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(highLightStr);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        // 2. 当前商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attrAgg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 得到属性的id
            attrVo.setAttrId(bucket.getKeyAsNumber().longValue());
            // 得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            // 得到属性的值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets()
                    .stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);
            // 添加属性
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // 3. 当前商品设计到的的品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 得到品牌id
            brandVo.setBrandId(bucket.getKeyAsNumber().longValue());
            // 得到品牌名
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            // 得到品牌图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);
            // 添加品牌
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 4. 当前商品涉及到的所有分类信息
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 得到分类id
            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
            // 得到分类名
            String catalogName = ((ParsedStringTerms) bucket.getAggregations().get("catalog_name_agg")).getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            // 添加分类
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        // 5. 分页信息
        result.setPageNum(param.getPageNum());
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int) total : ((int) total / EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages(totalPages);

        // 构造面包屑导航功能
        if (param.getAttrs() != null) {
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] split = attr.split("_");
                Long attrId = Long.parseLong(split[0]);
                navVo.setNavValue(split[1]);
                Optional<SearchResult.AttrVo> optional = attrVos.stream().filter(attrVo -> attrVo.getAttrId().equals(attrId)).findFirst();
                if (optional.isPresent()) {
                    navVo.setNavName(optional.get().getAttrName());
                } else {
                    navVo.setNavName(attrId.toString());
                }
                String encode = null;
                try {
                    encode = URLEncoder.encode(attr, "UTF-8");
                    encode = encode.replace("+", "%20");
                    encode = encode.replace("%2F", "/");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String link = param.getQueryString().replace("&attrs=" + encode, "");
                navVo.setLink("http://search.gulimall.com/list.html?" + link);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }
        return result;
    }
}
