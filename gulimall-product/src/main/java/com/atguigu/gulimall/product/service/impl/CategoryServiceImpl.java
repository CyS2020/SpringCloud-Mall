package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    private Gson gson = new Gson();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2. 组装成父子的树形结构
        return entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .peek(categoryEntity -> categoryEntity.setChildren(getChildren(categoryEntity, entities)))
                .sorted(this::categoryCompareTo)
                .collect(Collectors.toList());
    }

    @Override
    public void removeMenusByIds(List<Long> asList) {
        //TODO 检查当前删除的菜单，是否被别的地方引用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCategoryPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        findParentPath(catelogId, path);
        Collections.reverse(path);
        return path.toArray(new Long[0]);
    }

    /**
     * 级联更新所以关联的数据
     */
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Categories'"),
//            @CacheEvict(value = "category", key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Cacheable(value = "category", key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        // 数据库查询所有分类
        List<CategoryEntity> allEntities = baseMapper.selectList(null);
        // 查询1级分类
        List<CategoryEntity> l1Entities = getParentCid(allEntities, 0L);
        Map<String, List<Catelog2Vo>> map = new HashMap<>();
        for (CategoryEntity l1 : l1Entities) {
            List<Catelog2Vo> catelog2Vos = new ArrayList<>();
            // 查询2级分类
            List<CategoryEntity> l2Entities = getParentCid(allEntities, l1.getCatId());
            if (l2Entities != null && !l2Entities.isEmpty()) {
                for (CategoryEntity l2 : l2Entities) {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(l1.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = new ArrayList<>();
                    // 查询3级分类
                    List<CategoryEntity> l3Entities = getParentCid(allEntities, l2.getCatId());
                    if (l3Entities != null && !l3Entities.isEmpty()) {
                        for (CategoryEntity l3 : l3Entities) {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            catelog3Vos.add(catelog3Vo);
                        }
                    }
                    catelog2Vo.setCatalog3List(catelog3Vos);
                    catelog2Vos.add(catelog2Vo);
                }
            }
            map.put(l1.getCatId().toString(), catelog2Vos);
        }
        return map;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonCache() {
        // 1. 加入缓存逻辑
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            // 2. 缓存中没有
            return getCatalogJsonFromDbWithRedissonLock();
        }
        return gson.fromJson(catalogJSON, new TypeToken<Map<String, List<Catelog2Vo>>>() {
        }.getType());
    }

    // redisson加锁, 阻塞
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        RLock lock = redisson.getLock("catalogJson-lock");
        Map<String, List<Catelog2Vo>> dataFromDb;
        lock.lock();
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    // 手动加锁cas方式
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 60L, TimeUnit.SECONDS);
        Map<String, List<Catelog2Vo>> dataFromDb;
        if (lock) {
            System.out.println("获取分布式锁成功...");
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Lists.newArrayList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            System.out.println("获取分布式锁失败...进行重试...");
            return getCatalogJsonFromDbWithRedisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            return gson.fromJson(catalogJSON, new TypeToken<Map<String, List<Catelog2Vo>>>() {
            }.getType());
        }
        System.out.println("查询数据库...");
        // 查询所有分类
        List<CategoryEntity> allEntities = baseMapper.selectList(null);
        // 查询1级分类
        List<CategoryEntity> l1Entities = getParentCid(allEntities, 0L);
        Map<String, List<Catelog2Vo>> map = new HashMap<>();
        for (CategoryEntity l1 : l1Entities) {
            List<Catelog2Vo> catelog2Vos = new ArrayList<>();
            // 查询2级分类
            List<CategoryEntity> l2Entities = getParentCid(allEntities, l1.getCatId());
            if (l2Entities != null && !l2Entities.isEmpty()) {
                for (CategoryEntity l2 : l2Entities) {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(l1.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = new ArrayList<>();
                    // 查询3级分类
                    List<CategoryEntity> l3Entities = getParentCid(allEntities, l2.getCatId());
                    if (l3Entities != null && !l3Entities.isEmpty()) {
                        for (CategoryEntity l3 : l3Entities) {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            catelog3Vos.add(catelog3Vo);
                        }
                    }
                    catelog2Vo.setCatalog3List(catelog3Vos);
                    catelog2Vos.add(catelog2Vo);
                }
            }
            map.put(l1.getCatId().toString(), catelog2Vos);
        }
        // 3. 将查出的对象转为json放入缓存中
        String s = gson.toJson(map);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return map;
    }

    public synchronized Map<String, List<Catelog2Vo>> getCatalogJsonFromDbLocalLock() {
        return getDataFromDb();
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> allEntities, Long parentCid) {
        return allEntities.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }

    private void findParentPath(Long catelogId, List<Long> path) {
        // 收集当前节点id
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
    }

    // 递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid().equals(root.getCatId()))
                .peek(categoryEntity -> categoryEntity.setChildren(getChildren(categoryEntity, all)))
                .sorted(this::categoryCompareTo)
                .collect(Collectors.toList());
    }

    private int categoryCompareTo(CategoryEntity entity1, CategoryEntity entity2) {
        int val1 = entity1.getSort() == null ? 0 : entity1.getSort();
        int val2 = entity2.getSort() == null ? 0 : entity2.getSort();
        return val1 - val2;
    }

}