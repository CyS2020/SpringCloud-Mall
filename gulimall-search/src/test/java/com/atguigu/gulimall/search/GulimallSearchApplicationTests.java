package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

	@Autowired
	private RestHighLevelClient client;

	@Test
	public void contextLoads() {
		System.out.println(client);
	}

	@Test
	public void indexData() throws IOException {
		IndexRequest indexRequest = new IndexRequest("users");
		indexRequest.id("1");
		User user = new User("张三", 18, "男");
		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);

		IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		System.out.println(index);
	}

	@Test
	public void searchData() throws IOException {
		// 1. 创建检索请求
		SearchRequest searchRequest = new SearchRequest();
		// 指定索引
		searchRequest.indices("bank");
		// 指定DSL, 检索条件
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
		// 按照年龄值进行聚合
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		sourceBuilder.aggregation(ageAgg);
		// 计算平均薪资
		AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
		sourceBuilder.aggregation(balanceAvg);

		searchRequest.source(sourceBuilder);
		System.out.println("检索条件 : " + sourceBuilder);
		// 2. 执行检索
		SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

		// 3. 分析结果
		SearchHit[] searchHits = searchResponse.getHits().getHits();
		for (SearchHit hit : searchHits) {
			String str = hit.getSourceAsString();
			Accout accout = JSON.parseObject(str, Accout.class);
			System.out.println("账户信息 : " + accout);

		}
		Aggregations aggregations = searchResponse.getAggregations();

		Terms ageAgg1 = aggregations.get("ageAgg");
		for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
			System.out.println("年龄 : " + bucket.getKeyAsString());
		}
		Avg balanceAvg1 = aggregations.get("balanceAvg");
		System.out.println("平均薪资 : " + balanceAvg1.getValue());
	}

	@Data
	public class User {

		private String name;

		private Integer age;

		private String gender;

		public User(String name, Integer age, String gender) {
			this.name = name;
			this.age = age;
			this.gender = gender;
		}
	}

	@Data
	public static class Accout {

		private int account_number;

		private int balance;

		private String firstname;

		private String lastname;

		private int age;

		private String gender;

		private String address;

		private String employer;

		private String email;

		private String city;

		private String state;

	}

}
