package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * @author: CyS2020
 * @date: 2021/10/6
 * 描述：商城搜索接口
 */
public interface MallSearchService {

    SearchResult search(SearchParam searchParam);
}
