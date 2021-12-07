### 谷粒商城

![谷粒商城-微服务架构图](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E8%B0%B7%E7%B2%92%E5%95%86%E5%9F%8E-%E5%BE%AE%E6%9C%8D%E5%8A%A1%E6%9E%B6%E6%9E%84%E5%9B%BE.jpg?raw=true)

#### 关于项目
- 关于项目中所需要的数据库创建代码均放在data/sql目录下
- renren_fast与renren-generator是从码云上人人开源clone下来的, 时间2021.08.13
- renren-fast-vue前端代码所需的nodeJs安装的是14.17.5版本, 前后端均可以启动成功

#### 逆向工程
- 使用renren-generator工具生成五个微服务的crud的代码: controller; dao; entity; service
- 配置pom文件解决基本的依赖问题, 配置yml文件启动项目, 测试接口是否正常运行

#### Mybatis-plus依赖
- 导入依赖
```
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.2.0</version>
</dependency>
```
- 配置信息
    - 导入mysql驱动的依赖, 放在gulimall-common模块了
    - 在application.yml配置mysql数据源相关信息
    - 配置mybatis-plus: 配置@MapperScan扫描的包路径(项目启动类)
    - 配置mybatis-plus: 配置classpath扫描的xml文件路径(配置文件)
- 逻辑删除
    - 配置全局的逻辑删除规则(省略)
    - 配置逻辑删除的组件Bean(省略)
    - 给Bean(数据库的entity对象)加上逻辑删除注解@TableLogic
- 分页插件
    - 给项目添加MybatisConfig分页功能的配置项
- 数据库交互
    - service层实现类继承ServiceImpl<Dao, Entity>使用其baseMapper对象编写增删改查java代码与数据库交互
    - dao层接口继承BaseMapper<Entity>, 就是service层里的那个baseMapper; 拥有继承来的CRUD, 和自己定义的复杂交互方法
    - dao继承BaseMapper<T>该接口后,无需编写mapper.xml文件, 即可获得CRUD功能; 复杂的逻辑仍需最好还是编写xml文件
    - this.baseMapper就是实现了dao层接口的实例, 由框架自动注入进来的, 不需要我们手动实例化; (dao == mapper)

#### 微服务
- 注册中心: 每一个微服务上线后注册到注册中心, 对外提供服务;
- 配置中心: 每一个微服务的配置都很多, 集群环境需要一个个修改, 通过配置中心来管理修改;
- 网关: 前端请求通过网关进行 鉴权; 过滤; 路由. 由网关抵达微服务;
- nginx: 用户首先访问nginx, 将数据转发给网关, 静态资源存放在nginx里实现动静分离
- 动静分离: 静态--image, html, js, css等以实际文件存在的资源; 动--服务器需要处理的请求
- 浏览器 -> nginx -> 网关集群 -> 微服务 -> tomcat -> 调用程序

#### 技术搭配方案
- SpringCloud Alibaba - Nacos: 注册中心(服务发现/注册)
- SpringCloud Alibaba - Nacos: 配置中心(动态配置管理)
- SpringCloud - Ribbon: 负载均衡
- SpringCloud - Feign: 声明式 HTTP 客户端(调用远程服务)
- SpringCloud Alibaba - Sentinel: 服务容错(限流、降级、熔断)
- SpringCloud - Gateway: API 网关(webflux 编程模式)
- SpringCloud - Sleuth: 调用链监控
- SpringCloud Alibaba - Seata: 原 Fescar, 即分布式事务解决方案

#### Nacos服务注册
- 搭建服务注册所需的服务器, 提供了可视化界面
- 微服务中引入nacos-discovery依赖
- 微服务需要在yml文件中配置服务注册的服务器地址; 以及当前服务的名称
- 开启服务注册功能@EnableDiscoveryClient(可省略)

#### Nacos配置中心
- 和服务注册公用的服务器
- 微服务中引入nacos-config依赖
- 配置中心创建命名空间(可以省略, 使用默认的命名空间),
- 本地resource中创建bootstrap.properties文件配置 配置中心的服务器地址; 命名空间等--本地的
- 配置中心添加一个名叫 xxx.properties 的数据集, 服务名.properties(默认规则)--配置中心的
- bootstrap.properties文件可以配置初始加载文件(配置中心的某个文件)
- java代码中配置类动态刷新@RefreshScope, 获取某个配置的值@Value("${配置项}")
- 如果配置中心与本地配置文件中的冲突, 优先使用配置中心的配置

#### Nacos配置中心细节
- 命名空间: 用于进行租户粒度的配置隔离. 不同的命名空间下, 可以存在相同的Group或Data ID的配置; 默认public; 
开发, 测试, 生产中利用命名空间做环境隔离. 在bootstrap.properties配置命名空间的id
- 配置集: 一组相关或者不相关的配置项的集合称为配置集
- 配置集ID: 类似于以前的配置文件名application.yml
- 配置组: 默认所有的配置集都属于DEFAULT_GROUP组
- 使用细节: 每个微服务创建自己的命名空间; 使用配置分组来区分环境dev, test, prod;
- 同时加载多个配置集, 我们的任何配置文件都可以放在配置中心中, bootstrap.properties文件中配置需要加在的配置集
- @Value, @ConfigurationProperties...等从配置文件中获取值得使用方式仍然可用

#### OpenFeign远程调用
- 引入openfeign依赖
- 编写接口, 告诉springCloud这个接口需要调用的那个微服务
- 声明方法, 调用微服务的那个请求, 访问路径需要写全
    - 访问路径写法有两种一种是过网关(网关微服务 + /api/xx/xxx), 一种是不过网关(指定微服务 + /xx/xxx)
- 开启远程调用功能, @EnableFeignClients并传入扫描的包路径
- SpringCloud整个远程调用的逻辑
    - 如果有个Service调用了feign的Service, 并且传入了对象
    - 若有@RequestBody则将这个对象转为json
    - 去注册中心中找到该服务, 将json数据放在请求体的位置, 给对应的Rest接口发送请求
    - 对方服务收到请求以及请求体中的json数据
    - 将请求体中的json数据转为该接口接收的对象
- 若json数据模型是兼容的, 远程调用双方无需使用同一个TO
    
#### GateWay网关
- 创建一个模块作为项目的API网关微服务, 同时写需要引入注册中心与配置中心的功能
- 开启服务注册功能@EnableDiscoveryClient(可省略)
- 使用配置中心功能并进行配置与其他项目一样
- 启动失败是因为公共组件有Mybatis-plus因此需要配置数据源, 可以再启动项目的地方exclude掉

#### GateWay技术细节
- 功能: 路由, 鉴权, 限流
- Route: 网关的基本构建块即路由规则. 它由ID, 目标URI, 谓词集合和过滤器集合定义, 如果聚合谓词为真, 则匹配路由
- Predicate: 这是一个Java 8函数谓词, 这使您可以匹配来自HTTP请求的任何内容, 例如标头或参数
- Filter: 这些是GatewayFilter使用特定工厂构建的实例. 在这里您可以在发送下游请求之前或之后修改请求和响应
- Predicate如果满足某种规则才进行路由, Filter对谓词中的内容进行判断分析处理不是狭义的过滤, 可以是增删改操作

#### 网关路由与Nacos路由
- 不使用Nacos的时候, 网关路由规则uri后面直接写ip与端口, 使用了Nacos后面写微服务的名称

#### 文件存储系统
- 使用阿里云的-对象存储OSS; 也可以自己搭建服务器FastDFS, vsftpd
- 采用阿里云对象存储-服务端签名后直传
- 用户向后端服务器请求上传所需的Policy, 后端服务器返回Policy, 用户(前端)直接上传数据到OSS

#### 对象存储OSS
- 引入alicloud-oss依赖
- 配置文件中配置key, secret, endpoint等相关信息
- 自动注入OSSClient对象进行相关操作

#### JSR303数据校验
- 给Bean添加校验注解: javax.validation.constraints; 并定义自己的message提示
- 添加校验注解@Valid放在被校验的Bean前面; 效果: 校验错误会有默认的响应
- 给校验的Bean后面紧跟一个BindingResult, 就可以获得校验的结果
- 分组校验: @NotBlank(message = "品牌名必须填写", groups = {UpdateGroup.class, AddGroup.class})
    - 给实体类的字段校验注解添加groups表示什么情况需要校验
    - @Validated({AddGroup.class})给前端入参指定校验分组
    - 默认没有指定分组的校验注解@NotBlank, 在分组校验的情况下不生效如@Validated({AddGroup.class}), 在无分组校验的情况下生效即@Validated
- 自定义校验
    - 编写一个自定义的校验注解 
    - 编写一个自定义的校验器 ConstraintValidator
    - 关联自定义的校验器和自定义的校验注解, 一个校验注解可以设置多个校验器以进行不同类型的校验
      ```
      @Documented
      @Constraint(validatedBy = {ListValueConstraintValidator.class})
      @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
      @Retention(RetentionPolicy.RUNTIME)
      ```
  
#### 统一异常处理 @ControllerAdvice
- 编写异常处理类, 可以直接使用@RestControllerAdvice, 设置basePackages处理某些包
- 编写异常处理方法, 使用@ExceptionHandler, 设置value参数处理一种类型的异常
- 思想就是统一捕获异常然后处理无需try/catch了; 注意需要为不同的异常编写不同的错误码, 返回给前端;
- RestControllerAdvice会自动帮助catch,并匹配相应的ExceptionHandler, 然后重新封装异常信息, 返回值, 统一格式返回给前端. 

#### ElasticSearch搜索和数据分析引擎
- 基本概念
  - Index索引: 类似于mysql中的Database
  - Type类型: 类似于mysql中的Table
  - Document文档: 类似于mysql中的一条记录, json格式
  - 属性与属性值: 就是列名与列值
  - 倒排索引: 记录每个词条出现在哪些文档中, 检索时计算相关性得分
- 增删改查--存储功能
  - put请求/ip:port/索引/类型/id; post请求可以不带id自动生成
  - delete请求删除文档或删除索引, 注意没有删除类型这个接口
  - 使用put与post进行更新(同时新增); post/_update会对比原来的数据一样则什么也不做, put不能和_update组合在一起
  - get请求/ip:port/索引/类型/id;
- 信息检索方式
  - 一个是通过使用REST request url发送搜索参数(url+检索参数)
  - 一个是通过使用REST request body来发送它们(url+请求体)--常用
- 匹配查询--检索功能
  - match: 基本数据类型是精确匹配, 字符串类型是全文检索
  - match_phrase: 短语当成整个单词(不分词)进行检索
  - multi_match: 多个字段进行全文检索, 不管哪个字段包含了都算匹配上
  - term: 全文检索字段用match, 其他非text字段匹配用term
  - bool: 用来做复合查询; 搭配: must, should, must_not, filter(后两个不贡献相关性得分) 
- 执行聚合--分析功能
  - 聚合提供了从数据中分组和提取数据的能力. 最简单的聚合方法大致等于SQL GROUP BY和SQL聚合函数
- 映射
  - Mapping 是用来定义一个文档document, 以及它所包含的属性(field)是如何存储和索引的
  - 注释: 在7.0及以后得版本不支持type了数据直接保存在索引下边
- 分词器
  - 一个 tokenizer(分词器)接收一个字符流, 将之分割为独立的tokens(词元, 通常是独立的单词), 然后输出tokens流
  - 使用中文分词器: 安装插件elasticsearch-analysis-ik, 下载解压到elasticsearch/plugins/ik文件夹下
  - 自定义词库: 使用nginx服务器来存储自定义的字典, 然后修改/usr/share/elasticsearch/plugins/ik/config/中的 IKAnalyzer.cfg.xml中配置远程扩展字典地址
      ```
      <properties>
          <!--用户可以在这里配置远程扩展字典 -->
          <entry key="remote_ext_dict">http://192.168.0.102/es/fenci.txt</entry>
      </properties>
      ```

#### ElasticSearch客户端
  - 在docker容器中安装ElasticSearch服务并启动, 并安装Kibana可视化服务
  - 引入客户端依赖elasticsearch-client
  - 编写配置类能够访问远程的ElasticSearch服务器并向容器中注入RestHighLevelClient
  - 创建mapping映射关系, 即创建表及表中字段类型等, 然后才能增删改查数据
  - 使用RestHighLevelClient类参照API对ElasticSearch进行操作
  - `https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high.html`
  
#### 模板引擎
- 引入thymeleaf的starter依赖, 并在配置文件中关闭缓存
- 静态资源都放在static文件夹下就按照路径直接访问
- 页面放在templates文件夹下是可以直接访问的, springBoot访问项目时默认会找index.html; 其他html需要编写Controller返回
- 页面修改无需重启服务器需要额外引入dev-tools依赖, 然后ctrl + shift + f9自动重新编译页面
- yml配置文件中关闭thymeleaf缓存; 

#### nginx + windows搭建域名访问环境
- 正向代理: 科学上网等, 隐藏客户端信息; 帮助我访问外界
- 反向代理: 屏蔽内网服务器信息, 负载均衡访问; 帮助外界访问我
- 可以通过记事本修改hosts文件来将域名与ip地址绑定, 访问gulimall.com时跳转到虚拟机的ip地址访问nginx服务器
```
//C:\Windows\System32\drivers\etc\hosts
192.168.0.102 gulimall.com
192.168.0.102 search.gulimall.com
```
- 通过nginx反向代理将请求负载均衡的转发到网关, nginx.conf中配置上游服务器, 网关服务有几个就配置几个
```
http {
    upstream gulimall{
      server 192.168.0.101:88; // 注意该位置配置的是正确的服务器地址, wifi环境下经常变化
    }
    include /etc/nginx/conf.d/*.conf;
}
```
- conf.d文件夹添加代理配置, 该文件夹下的所有配置文件都回包含在总配置文件中
- nginx在将请求代理给网关的时候会丢失请求的Host, 需要配置nginx不要丢掉该信息
- nginx配置动静分离, 所有的静态资源均由nginx返回, 并配置资源地址
- gateway的路由规则需要配置, 根据不同的域名转发到不同微服务上
```
listen       80;
server_name  gulimall.com  *.gulimall.com;

location /static/ {
    root /usr/share/nginx/html;
}

location / {
    proxy_set_head Host $host;
    proxy_pass http://gulimall;
}
```

#### JMeter压力测试
- 性能指标
  - 响应时间(Response Time: RT): 从客户端发起请求到客户端接收到服务端的返回, 整个过程的时间
  - HPS(Hits Per Second): 每秒点击次数, 单位是次/秒
  - TPS(Transaction per Second): 系统每秒处理交易数, 单位是笔/秒
  - QPS(Query per Second): 系统每秒处理查询次数, 单位是次/秒
  - 最大响应时间(Max Response Time): 指用户发出请求或者指令到系统做出反应(响应)的最大时间
  - 最少响应时间(Min ResponseTime): 指用户发出请求或者指令到系统做出反应(响应)的最少时间
  - 90%响应时间(90% Response Time): 是指所有用户的响应时间进行排序, 第90%的响应时间
- 从外部看, 性能测试主要关注如下三个指标
  - 吞吐量: 每秒钟系统能够处理的请求数、任务数
  - 响应时间: 服务处理一个请求或一个任务的耗时
  - 错误率: 一批请求中结果出错的请求所占比例
  
#### 性能优化
- JVM内存 = (本地方法栈 + 虚拟机栈 + 程序计数器) + (堆区 + 元数据区)
  - 单个微服务来说要优化cpu占用与内存占用
  ![JVM内存结构](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/JVM%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%84.png)
- 中间件越多, 性能损失越大, 每一个中间件吞吐量就要下降一点(nginx -> 网关集群 -> 微服务)
  - 调整中间件性能使得本身的吞吐量增大, 让网络交互的更快(网卡, 网线, 传输协议)
- 业务层面的优化: Mysql优化, 模板渲染速度, 静态资源获取
- 使用工具jvisualvm(visual GC)、JMeter等分析工具测试并发性能

#### Redis缓存
- 适合放入缓存的数据: 即时性, 数据一致性要求不高的; 访问大, 更新频率不高的(读多, 写少)
- docker容器安装并启动redis, 同时下载Another Redis Desktop Manager客户端进行可视化操作
- 项目中引入依赖data-redis, 并在yml配置文件中配置数据源ip地址与端口号
- 使用SpringBoot自动配置好的StringRedisTemplate进行操作, opsForXXX, boundXXXOps等操作
- redis中的数据类型其实是针对于K-V中的V来说的, V可以为Value, Hash, List, Set, ZSet
- 

#### SpringCache管理缓存
- 项目中引入依赖data-redis与cache, 均为starter组件
- 自动配置中CacheAutoConfiguration会导入RedisCacheConfiguration, 自动配置了缓存管理器RedisCacheManager
- 所以我们只需要在yaml配置spring.cache.type=redis; 使用redis作为缓存
- 开启缓存功能@EnableCaching; 并在方法上添加注解对返回结果进行相应的缓存操作
- 通过使用注解来使用缓存功能
  - @Cacheable: 触发将数据保存到缓存的操作
  - @CacheEvict: 触发将数据从缓存删除的操作; 失效模式
  - @CachePut: 不影响方法执行的方式更新缓存; 双写模式
  - @Caching: 组合以上多个操作
  - @CacheConfig: 在类级别共享缓存的相同配置
- 每个需要缓存的数据我们都来在注解参数里指定缓存分区(按照业务类型划分)
- 默认行为需要自定义参考CacheConfig类与yml配置文件
  - 如果缓存中存在, 则方法不调用
  - key是默认自动生成的; 自定义缓存生成的key; 使用注解的key参数指定spEL
  - value默认使用jdk序列化然后存到缓存中; 将数据保存为json格式
  - 默认时间是-1, 永不过期; 指定缓存数据的存活时间; 在yaml配置文件中修改ttl
  - 更改缓存的配置, 只需要给容器中放入一个RedisCacheConfiguration应用到所有的缓存分区中
- 配置文件与配置类是绑定的, 使用的时候需要放在容器中的; (可省略)
```
@ConfigurationProperties(prefix = "spring.cache")
@EnableConfigurationProperties(CacheProperties.class);
```
- 删除多个缓存可以使用@Caching组合操作, 可以指定@CacheEvict里面的allEntries参数设为true
- 最佳实战: 存储同一类型的数据, 都可以指定同一个分区, 可以批量删除

#### 缓存问题及解决方案
- 缓存穿透: 将null结果缓存, 并加入短暂的过期时间
- 缓存雪崩: 在原有的失效时间基础上添加随机值, 例如1-5分钟随机
- 缓存击穿: 先加本地锁查数据库, 查到以后放入缓存并释放锁; 双重检查(获取锁后再去缓存中确定一下) + 原子操作(锁要范围包含查mysql, 写redis)
- 分布式下加分布式锁: 加锁值设置为uuid(唯一id), 并设置过期时间, 设置成功返回true; 解锁使用lua脚本保证原子性; 使用StringRedisTemplate实现
```
// 加锁与设置过期时间需要保证原子性
Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 60L, TimeUnit.SECONDS);
// 解锁(删锁)某个键为lock值uuid的锁, 判断与删除也要保证原子性
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
Long val = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Lists.newArrayList("lock"), uuid);
```

#### 缓存数据一致性
- 双写模式: 修改数据库, 然后修改缓存
- 失效模式: 修改数据库, 删除缓存
- 用户维度那么没有这么高的并发量, 基础数据对数据实时性要求不高, 都可以通过缓存 + 过期时间足够用了; 如果觉得不够用再用分布式读写锁就好了
- 遇到实时性一致性高的场景就应该查数据库, 即使慢点
- 完美解决: 数据异步同步, mysql会将操作记录在Binary log日志中, 通过canal去监听数据库日志二进制文件, 解析log日志, 同步到redis中进行增删改操作
- 注释: 关于canal还可以解决数据异构的问题, 监听不同用户的访问记录生成用户推荐表

#### springCache不足
- 读模式
  - 缓存穿透: 缓存空数据, yml配置spring.cache.redis.cache-null-values=true
  - 缓存击穿: 加锁, @Cacheable中参数sync = true控制this锁
  - 缓存雪崩: 加上过期时间(不用随机), yml配置spring.cache.redis.time-to-live=3600000
- 写模式(数据一致性), springCache并没有考虑
- 总结: 常规数据(读多写少, 即时性, 实时性要求不高的数据), 完全可以使用springCache; 特殊数据需要特殊设计

#### Redisson分布式锁
- 项目中引入redisson依赖, 并进行配置RedissonConfig
- 使用RedissonClient作为客户端对Redis进行操作
- 可以使用该组件当做分布式的juc包, 本项目不会直接使用该组件处理缓存一致性

#### Redisson分布式锁细节
- 实现了juc包下的各种高级锁功能, 且实现了juc包下接口无缝衔接; 只要key一样就是同一把锁
- 熟悉使用互斥锁, 读写锁, 信号量(分布式限流), CountDownLatch, Condition
- 默认加锁时间是30s, 锁会自动续期, 如果业务超长则自动续上新的30s; 无需担心业务时间长锁被过期清理
- 加锁的业务执行完成后不会再给锁自动续期了; 即使不手动解锁, 锁默认在30s后自动删除
- 如果指定了锁的超时时间, 锁到期后不会自动续期; 自动解锁时间一定要大于业务执行时间
- 最佳实战: lock.lock(30, TimeUnit.SECONDS); 省掉续期操作, 并手动解锁

#### 短信验证码
- 使用阿里云市场中的某个短信服务, 参照文档中的实例代码进行验证码的发送
- Nacos配置中心里配置好必要的参数 host, path, appcode(最重要), templateId 等
- 验证码两个问题 
  - 接口防刷
  - 验证码校验: 使用redis保存验证码一是防止短时间内重复发送验证码二是用于特定时间内校验

#### MD5加密
- Message Digest algorithm 5 信息摘要算法
- 压缩性, 容易计算性, 抗修改性, 强抗碰撞, 不可逆性
- 加盐: 随机数与MD5生成字符串进行组合; 数据库同时存储MD5值与salt值
- MD5不能直接用于密码的直接存储, 彩虹表的会暴力破解
- 使用spring中的BCryptPasswordEncoder进行加密; 盐与MD5值放在一起了, 但是你不知道那部分是盐

#### springSession管理session
- 引入session-data-redis依赖, 所有需要使用session的模块都要依赖并且配置
- 配置session存储类型为redis, 过期时间30m等各种配置项
- @EnableRedisHttpSession开启springSession功能, 并在controller里面设置session属性值
- 设置其他配置属性GulimallSessionConfig, 包括序列化方式, 修改作用域为父域等配置

#### springSession核心原理
- @EnableRedisHttpSession注解导入了RedisHttpSessionConfiguration配置
- 给容器中添加了一个组件RedisOperationsSessionRepository, 是redis操作session的DAO, 进行session增删改查
- 给容器中添加了一个过滤器SessionRepositoryFilter, 创建的时候就导入了RedisOperationsSessionRepository
- 原始的request, response都被包装成如下对象; 以后获取session都要获取request.getSession()相当于调用的wrappedRequest.getSession()
- wrappedRequest.getSession()是从RedisOperationsSessionRepository中获取的, 就是从redis中获取的; 主要是装饰者设计模式
- 网站交互session自动续期, 浏览器关闭session在设定的过期时间内过期
```
protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    request.setAttribute(SESSION_REPOSITORY_ATTR, this.sessionRepository);
    
    SessionRepositoryRequestWrapper wrappedRequest = new SessionRepositoryRequestWrapper(
            request, response, this.servletContext);    // 包装request
    SessionRepositoryResponseWrapper wrappedResponse = new SessionRepositoryResponseWrapper(
            wrappedRequest, response);    //包装response

    try {
        filterChain.doFilter(wrappedRequest, wrappedResponse);
    }
    finally {
        wrappedRequest.commitSession();
    }
}
```

#### RabbitMQ消息中间件
- docker安装并启动rabbitmq, 15672端口提供了可视化界面
- 在yml配置文件中配置连接信息ip地址与端口号以及虚拟主机等信息, 编写配置RabbitMqConfig类设置序列化类型
- 项目引入依赖amqp依赖, RabbitAutoConfiguration会自动生效
- 使用SpringBoot自动配置好的RabbitTemplate、amqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate进行操作
- 使用@EnableRabbit开启RabbitMQ功能
- 使用amqpAdmin创建Exchange、Queue、Binding; 使用RabbitTemplate发送消息(对象必须实现序列化接口)
- 使用@RabbitListener(queues = "xx", "xxx")监听消息, 必须启用该注解@EnableRabbit; 方法接收参数1. Message、2. T、3. Channel
- 服务启动多个同一个消息也只能有一个服务进行处理; 一个消息处理结束服务才会接收下一个消息
- 使用方式: @RabbitListener(类+方法)--监听队列; @RabbitHandler(方法)--重载方法区分不同的消息类型
![RabbitMQ原理](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/RabbitMQ%E5%8E%9F%E7%90%86.PNG?raw=true)

#### RabbitMQ可靠抵达
- 服务器收到消息就回调: 配置文件 + 设置确认回调-配置类RabbitMqConfig
```
spring.rabbitmq.publisher-confirms=true
rabbitTemplate.setConfirmCallback()
```
- 消息没有正确抵达队列回调: 配置文件 + 设置确认回调-配置类RabbitMqConfig
```
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.template.mandatory=true
rabbitTemplate.setReturnCallback()
```
- 设置手动ack, 自动确认模式下消息会丢失: 配置文件 + 编码手动确认逻辑
```
spring.rabbitmq.listener.simple.acknowledge-mode=manual
Channel.basicAck()
Channel.basicNack()
Channel.basicReject()
```

#### 分布式事务
- 使用@Transactional注解开启本地事务, 最常使用的三个参数: readOnly、propagation、isolation
- 本地事务在分布式环境下, 只能控制自己的回滚, 控制不了其他服务的回滚
- 产生分布式事务最大原因就是网络问题(抖动) + 分布式机器(无法控制别人的机器), 无法感知远程分布式服务是真失败还是假失败

#### 使用Seata控制分布式事务
- 每个微服务必须先创建undo_log表(回滚日志表)
```
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```
- 安装事务协调器: seata-server服务器;
- 启动之前先配置一下注册中心registry.conf的类型与地址; 与配置中心file.conf的事务存储位置(本项目使用默认配置)
```
type = "nacos"
nacos {
    serverAddr = "localhost:8848"
    namespace = "public"
    cluster = "default"
  }
```
- 项目中引入seata依赖; 并启动seata服务器
- 所有想要用到分布式事务的微服务使用seata DataSourceProxy进行数据源代理, SeataConfig配置类
- 每个微服务都要导入registry.conf与file.conf文件, 并在file.conf里配置如下参数
```
# 占位符填微服务的名字例如 gulimall-order
vgroup_mapping.${application.name}-fescar-service-group = "default"
```
- 给分布式大事务的入口添加@GlobalTransactional注解, 每一个小事务使用@Transactional即可
- 注意Seata不同版本或有区别, 使用时请参考官方文档
![Seata原理](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/Seata%E5%8E%9F%E7%90%86.PNG?raw=true)

#### 分布式事务模式
- AT: 在一些无需高并发系统可以使用, 例如后台管理系统的大保存方法public void saveSpuInfo(SpuSaveVo vo)
- TCC: 也不适合高并发场景
- 高并发场景: 柔性事务-最大努力通知型方案; 柔性事务-可靠消息+最终一致性方案(异步确保型)
- 使用RabbitMq延时队列实现: 柔性事务-可靠消息+最终一致性方案(异步确保型)

#### 定时任务
- spring中定时任务由六位组成, 不允许第七位的年
- 在周的位置 1-7 代表周一至周日; 也可以使用MON-SUN
- 定时任务不应该阻塞, 默认是阻塞的;
  - 让业务以异步的方式执行, 提交到某个线程池中
  - 提交到定时任务线程池: 设置TaskSchedulingProperties配置项 `spring.task.scheduling.pool.size=5`
- 定时任务自动配置类: TaskSchedulingAutoConfiguration; 属性绑定TaskSchedulingProperties
- 定时任务 + 异步任务实现 = 定时任务不阻塞功能

#### 异步任务
- 让定时任务异步执行: 使用@EnableAsync开启异步执行功能, 方法上使用@Async进行异步执行
- 异步任务自动配置类: TaskExecutionAutoConfiguration; 属性绑定TaskExecutionProperties
```
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=50
```

#### 秒杀服务
- 需要商品随机码, 只有拿到随机码才能参与秒杀服务, 防止非公平抢购
- 分布式锁来处理上架幂等性问题, 多个微服务的定时任务防止重复上架
- 分布式信号量进行限流, 商品秒杀总量作为分布式信号量, 上架时设置信号量

#### 秒杀系统设计
- 服务单一职责 + 独立部署: 秒杀服务即使自己扛不住压力挂掉, 不要影响别人
- 秒杀链接加密: 防止恶意攻击, 模拟秒杀请求, 1000次/s攻击; 防止链接暴露, 自己工作人员, 提前秒杀商品(随机码就在这用)
- 库存预热 + 快速扣减: 秒杀读多写少, 无需每次实时校验库存, 我们库存预热, 放到redis中, 信号量控制进来秒杀的请求(分布式信号量)
- 动静分离: nginx做好动静分离. 保证秒杀和商品详情页的动态请求才打到后端的服务集群. 使用CDN网络, 分担本集群压力
- 恶意请求拦截: 识别非法攻击请求并进行拦截, 网关层进行拦截
- 流量错峰: 使用各种手段, 将流量分担到更大宽度的时间点。比如验证码, 加入购物车等
- 限流&熔断&降级: 前端限流 + 后端限流限制次数. 限制总量, 快速失败降级运行, 熔断隔离防止雪崩
- 队列削峰值: 1万个商品, 每个1000件秒杀. 双11所有秒杀成功的请求, 进入队列, 慢慢创建订单, 扣减库存即可

#### 无需回滚的方式
- 自己在方法内部catch掉, 异常不往外抛出

### 业务知识
#### spu与sku
- SPU(Standard Product Unit): 标准化产品单元. 是商品信息聚合的最小单位, 是一组可复用、易检索的标准化信息的集合, 该集合描述了一个产品的特性
- SKU(Stock Keeping Unit): 即库存进出计量的基本单元, 可以是以件,盒,托盘等为单位; SKU这是对于大型连锁超市配送中心物流管理的一个必要的方法. 现在已经被引申为产品统一编号的简称, 每种产品均对应有唯一的SKU号
- 基本属性(商品介绍、规格与包装)都是spu属性; 销售属性是sku属性; 且规格参数可以提供检索
- 属性与属性的分组都是以三级分类组织起来的, 每个三级分类下的商品共享规则参数与销售属性, 不一定全部用到
- 三级分类下的商品有着共同的spu, sku属性key, 不同的是spu, sku属性的value;
- 三级分类表 -> 属性分组表 -> (属性分组&属性关联表)-> 属性表; 商品的属性值存储在商品属性值表、销售属性值表里

#### Object 划分
- PO(persistent object)--持久对象
  - PO 就是对应数据库中某个表中的一条记录, 多个记录可以用PO的集合, PO中应该不包含任何对数据库的操作
- DO(Domain Object)--领域对象
  - 就是从现实世界中抽象出来的有形或无形的业务实体. 
- TO(Transfer Object)--数据传输对象
  - 不同应用程序之间传输的对象
- DTO(Data Transfer Object)--数据传输对象
  - 泛指用于展示层与服务层之间的数据传输对象
- VO(View Object)--视图对象
  - 接受页面传递来的数据封装对象; 将业务处理完成的对象封装成页面要用的数据
- BO(business Object)--业务对象
  - 主要作用是把业务逻辑封装为一个对象. 这个对象可以包括一个或多个其它的对象
- POJO(Plain Ordinary Java Object)--简单无规则java对象
  - 我的理解就是最基本的java Bean, 只有属性字段及setter和getter方法, POJO是DO/DTO/BO/VO的统称
- DAO(Data Access Object)--数据访问对象
  - DAO中包含了各种数据库的操作方法, 通过它的方法结合PO对数据库进行相关的操作, 夹在业务逻辑与数据库资源中间配合VO, 提供数据库的CRUD操作

#### 数据库表
- 三级分类 -> spu -> sku -> 属性分组 -> 具体属性 -> 具体属性的值
- 属性与属性分组表会记录自己的三级分类信息; 属性与属性分组有关联关系表; 品牌与分类有关联关系表

#### ES保存商品信息
- 商品上架时保存spu下的所有sku, 以及sku的品牌与分类信息, 以及可以被检索的基本属性
- ES搜索的时候全文匹配使用must搜索(参与评分), 其他都使用filter过滤(不参与评分)
- 查询的时候需要完成功能: 模糊匹配, 过滤(按照属性, 分类, 品牌, 价格区间, 库存), 排序, 分页, 高亮功能, 聚合分析
- 用户在搜手机的时候模糊匹配, 然后确定手机分类(图书等其他分类也有的), 确定苹果华为小米等品牌, 确定屏幕摄像头cpu等属性, 再确定价格区间, 有现货不要预约的, 综合排序展示; 聚合分析和用户无关
- 如果是嵌入式的属性, 查询, 聚合, 分析都应该使用嵌入式的方式(使用嵌入式是为了避免数组类型数据扁平化)

#### OAuth2.0 社交登录交互
```
我  -----向用户申请请求认证---->     resource owner(用户本人)
们                                         |
的                                   使用授权进行认证
应                                         ↓
用  <----认证通过返回访问令牌----- Authorization Server(QQ服务器)
程  ----使用访问令牌获取开放保护信息----> Resource Server(QQ服务器)
序  <----认证令牌返回受保护的信息----- Resource Server(QQ服务器)
```
#### 微博授权认证步骤
- 引导需要授权的用户到如下地址
```
https://api.weibo.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI
```
- 如果用户同意授权, 页面跳转至YOUR_REGISTERED_REDIRECT_URI/?code=CODE; 获取Code只能使用一次
- Code换取Access Token, 其中client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET可以使用basic方式加入header中, 返回值
```
// post请求
https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
// 返回报文
{
    "access_token": "2.00d2F6FGHOKiVB05b2d6f0d109Dp_S",
    "remind_in": "157679999",
    "expires_in": 157679999,
    "uid": "5576657433",
    "isRealName": "true"
}
```
- 使用获得的Access Token调用API(接口管理中的已有权限); 同一个用户的Access Token一段时间内是不变化的
- 有关登录问题参考文件: <08、单点登录与社交登录.pdf> 文件

#### cookie session 跨域
- cookie不安全session安全, 后端开发只操作session, session与cookie不分家, session就是用到了cookie来实现的, cookie是实现Session的一种方式
- 服务端需要通过session来识别具体的用户, 服务端要为特定用户创建特定的session, 用于标识这个用户并且跟踪
- 那么问题来了session如何来识别具体的用户呢？客户端会将cookie信息发送到服务端, cookie里面记录一个Session ID(字段jsessionid)
- session是抽象的概念, cookie是具体的概念, cookie是session一种具体的实现方式
- 会话跟踪cookie与session, 可以理解为cookie是一个箱子, 里面可以填内容信息；如果填具体信息那就是cookie客户端机制, 如果是填sessionId具体信息存在服务器则是session机制
- 在很多操作中都需要检查用户是否登录因此通过在代码中编写拦截器进行预检查(实现HandlerInterceptor.preHandle())还需配置拦截哪些url; 另外还可以用AOP的方式拦截
```
       前端        ->           后端
cookie(sessionId)  ->   session(HttpSession)
```

#### 登录拦截
- 编写拦截器实现HandlerInterceptor接口, 并实现preHandle、postHandle等方法
- 编写配置类实现WebMvcConfigurer接口, 并实现addInterceptors方法, 托管spring
- 往addInterceptors方法中添加上述编写好的拦截器和拦截路径的规则

#### session共享问题与解决办法
- 不能跨不同域名共享, 多系统登录的共享问题
- 解决办法1: 单点登录技术, 使用中央认证服务器
- 集群环境下一个微服务会部署到多个服务器上, session不能同步
- 解决办法1: hash一致性; 使用ip地址或者业务字段进行hash
- 解决办法2: 后端统一存储session; 使用mysql或者redis
- 不同微服务服务, 子域session共享; jsessionid这个cookie默认是当前域名
- 解决办法1: 返回jsessionid的时候设置作用域为父域, 放大作用域

#### 单点登录技术
- 给中央认证服务器留下登录痕迹, 使用redis保存用户登录信息, 浏览器cookie中保存token(中央认证服务器的域)
- 中央认证服务器要将token信息在重定向的时候放在url上面
- 其他系统服务器要处理url上的token信息, 去中央服务器检验通过后会返回用户信息; 只要有token就该保存到自己的session中
- 当前系统将用户信息保存在自己的会话中(用户信息由中央服务器返回); 后面操作无需跳转到中央认证服务器了
- 其他系统访问时会跳转到中央认证服务且会带上浏览器cookie中的token的不需要重新登录了, 又会重定向到该系统
- 更多详细步骤参见 08、单点登录与社交登录.pdf 文档

#### 订单中心
- 电商系统涉及到3流, 分别时信息流, 资金流, 物流; 而订单系统作为中枢将三者有机的集合起来
- 订单模块是电商系统的枢纽, 在订单这个环节上需求获取多个模块的数据和信息, 同时对这些信息进行加工处理后流向下个环节, 这一系列就构成了订单的信息流通
- 订单的状态: 待付款 -> 已付款/待发货 -> 已发货/待收货 -> 已完成 -> 已取消 -> 售后中
- 订单Id等各种Id生成可以使用mybatis中的IdWorker类进行生成, 底层原理为雪花算法
![订单中心](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E8%AE%A2%E5%8D%95%E4%B8%AD%E5%BF%83.PNG?raw=true)
- 订单流程是指从订单产生到完成整个流转的过程, 从而行程了一套标准流程规则, 可概括如下图
![订单流程](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E8%AE%A2%E5%8D%95%E6%B5%81%E7%A8%8B.PNG?raw=true)

#### 接口幂等性
- 订单的提交需要保证幂等性, 使用令牌机制来实现幂等性, 前端token(后端生成返给前端的)与后端redis中(后端生成时保存的)的token
- 校验令牌和删除令牌的时候需要保证原子性, 同分布式缓存中的lua脚本一样, 直接拿来用即可; val为1则成功, 0则失败
```
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
Long val = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Lists.newArrayList("lock"), orderToken);
```
- 参见 02、接口幂等性.pdf 文档

#### 使用RabbitMQ实现定时关单与库存解锁的分布式事务
- 发送消息的时机: 1. 订单创建成功后就会发送(不管会不会支付), 2. 库存锁定成功后就会发送(不管整个事务是否成功) 
- 下单成功订单过期没有支付被系统自动取消, 或者被用户手动取消, 都需要关闭订单;OrderCloseListener类
  - 根据rabbitMQ监听消息中**订单信息**查询数据库的这条**订单信息对应**的记录
  - 若当前数据库订单状态为**待付款**状态, 则将订单状态改为**已取消**状态, 便于库存服务根据该状态解锁库存
  - 定时关单的延时为30min, 若30min内未支付则默认状态一直为**待付款**, 到达时间就会修改数据库的订单状态为**已取消**
  - 库存解锁的延时为50min, 因此库存来查询订单的状态的时候若为已取消则进行自动解锁
- 下单成功, 库存服务锁定成功, 接下来的业务调用失败(事务中的其他业务调用), 导致订单回滚, 需要自动解锁库存;StockReleaseListener类
  - 根据rabbitMQ监听消息中**锁库存工作单的id**查询数据库的这条**锁库存工作单**的记录
  - 若没有锁库存记录则代表锁库存失败库存也回滚了(库存锁定的修改记录与工作单的新增记录一起回滚了), 但是成功发送了锁库存工作单的消息, 这种情况无需解锁 
  - 若有锁库存记录也不一定都要解锁: 若订单没有创建则需要解锁库存--库存锁成功了订单创建失败了; 订单状态**已取消**则需要解锁库存--订单未支付或者手动取消了
  - 手动确认消息, 解锁成功则会删除该消息, 解锁失败重回消息队列后续在尝试进行解锁操作
- 若因为机器卡顿网络延迟等问题造成库存解锁消息先执行, 定时关单后执行那么该订单的库存, 库存查询订单状态为待支付则不解锁, 该订单库存则永远也无法解锁了
- 在定时关单成功后, 再发一个消息给解锁库存的消息队列中, 解锁库存有两种消息一个是定时关单时候发的一个是下单成功时候发的
- 定时关单发的消息是解锁库存的主要逻辑, 下单成功发送的消息是解锁库存的补偿逻辑; 补偿逻辑解锁库存前会检查库存工作单是否已解锁, 若已解锁则啥也不做

#### RabbitMQ业务的应用
![消息队列流程](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E6%B6%88%E6%81%AF%E9%98%9F%E5%88%97%E6%B5%81%E7%A8%8B.jpg?raw=true)

#### 支付异步通知的验签
- 接收支付宝发来的异步通知以及验签使用OrderPayedListener类
- 支付宝异步通知地址为`http://cys-mall.natapp1.cc/payed/notify`
- 有内网穿透工具`Forwarding http://cys-mall.natapp1.cc -> order.gulimall.com:80`
- 请求达到主机, 结合本地配置的etc/hosts域名地址, 将order.gulimall.com映射到虚拟机ip上
- 虚拟机收到请求转给nginx, 在nginx修改请求头(一定要配置), nginx转给网关, 网关转给订单服务
- 订单服务收到后先进行验签后进行业务操作

#### 支付收单
- 订单在支付页, 不支付, 一直刷新, 订单过期了才支付, 订单状态改为已支付了, 但是库存解锁了
  - 使用支付宝自动收单功能解决timeout参数. 只要一段时间不支付, 就不能支付了
- 由于时延等问题. 订单解锁完成, 正在解锁库存的时候, 异步通知才到
  - 订单解锁, 手动调用收单, 不再允许支付了
- 网络阻塞问题, 订单支付成功的异步通知一直不到达
  - 查询订单列表时, ajax获取当前未支付的订单状态, 查询订单状态时, 再获取一下支付宝此订单的状态
- 其他各种问题
  - 每天晚上闲时下载支付宝对账单, 一一进行对账
  
#### 秒杀业务流程
- 秒杀只是商品优惠显示信息, 价格做了优惠, 流程和正常购买一致; 优点是流量比较分散, 业务也比较统一; 缺点是流量级联的传递到其他系统
![秒杀业务流程1](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E7%A7%92%E6%9D%80%E4%B8%9A%E5%8A%A1%E6%B5%81%E7%A8%8B1.png?raw=true)
- 秒杀系统拥有一套独立的业务流程, 且不操作数据库; 优点是能抗住高并发, 缺点是系统需要一套独立的业务处理流程
- 耗时统计在10ms, 一秒一个线程可以处理100个请求, tomcat最大线程数若为500, 那么每秒则能处理5万并发量; 20个单机集群就能处理100万的并发了
![秒杀业务流程2](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E7%A7%92%E6%9D%80%E4%B8%9A%E5%8A%A1%E6%B5%81%E7%A8%8B2.PNG?raw=true)

#### Sentinel熔断&降级&限流
- 项目中引入依赖sentinel, 本项目每个微服务都需要因此在common里引入
- 下载sentinel Dashboard控制台(版本对应上), 以jar包形式运行
- 在控制台中调整参数, 设置`流控`、`降级`、`授权`、`热点`等流控规则; 默认所有设置保存在微服务内存中, 重启失效
- sentinel控制台的实时监控没有图表数据, 自定义请求限流以后返回的降级数据
  - 项目中引入依赖actuator(springBoot高版本自带), 并在配置中允许Endpoints的访问: `management.endpoints.web.exposure.include=*`
  - 添加配置类SentinelConfig, 自定义请求限流后的返回数据
- sentinel Dashboard配置**流控**规则: 资源名、针对来源、阈值类型、单机阈值、是否集群、流控模式、流控效果
  - 项目中引入sentinel和openfeign, **调用方**配置sentinel对feign的支持: `feign.sentinel.enabled=true`
  - 实现远程调用接口SeckillFeignServiceFallback, 实现熔断回调方法, 在远程调用失败的时候返回熔断数据--**熔断**保护
- sentinel Dashboard配置**降级**规则: 资源名、降级策略、RT、时间窗口; 
  - 通过上述配置调用方手动指定服务降级策略, 远程服务被降级处理触发熔断回调方法
  - **被调用方**即服务的提供方, 在应对超大并发量的时候, 也可以指定降级规则; 提供方服务在运行中, 但不运行业务逻辑, 返回降级数据(限流数据)
- 熔断主要是在调用方控制, 降级是在提供方控制. 熔断主要是防止提供方宕机, 降级则是提供方为了解压, 给调用方提供了一些简单的数据
- 使用方法自定义受保护的形式时, try-catch还是@SentinelResource都需要定义限流后的返回数据; url请求可以使用统一的返回SentinelConfig
  - 自定义受保护资源一: try(Entry entry = SphU.entry("资源名")){}catch ((BlockException e)){}
  - 自定义受保护资源二: @SentinelResource 注解定义资源并配置blockHandler和fallback函数来进行限流之后的处理
- 网关项目中引入依赖sentinel且引入依赖sentinel-gateway, 就可以从网关的层面进行限流

#### Sleuth与Zipkin服务链路追踪
- 项目中引入依赖sleuth, 本项目每个微服务都需要因此在common里引入, 配置日志打印级别即可在日志文件查看调用链日志
- 项目中引入依赖zipkin, 本项目每个微服务都需要因此在common里引入, 在docker中安装zipkin服务, 链路追踪数据会汇报给该服务器
- Sleuth主要服务链追踪原理图
![Sleuth链路追踪](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/Sleuth%E9%93%BE%E8%B7%AF%E8%BF%BD%E8%B8%AA.png?raw=true)
- Zipkin可视化观察原理图
![Zipkin原理图](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/Zipkin%E5%8E%9F%E7%90%86%E5%9B%BE.PNG?raw=true)

### 拦路虎
#### Nacos启动失败
- 修改startup.cmd文件, 默认使用集群模式启动, 可以将启动模式改为set MODE="standalone"

#### 前后端的访问
- 1.前端会配置请求的前缀(网关的地址和端口), 例如前端发来请求 http://localhost:88/api/captcha.jpg
```
window.SITE_CONFIG['baseUrl'] = 'http://localhost:88/api';
```
- 2.网关需要转到对应的微服务进行如下配置(lb代表负载均衡) http://localhost:8080/renren-fast/captcha.jpg
```
gateway:
  routes:
    - id: admin_route
      uri: lb://renren-fast
      predicates:
        - Path=/api/**
      filters:
        - RewritePath=/api/?(?<segment>.*), /renren-fast/$\{segment}
```
- 3.后端的路径拼接时候前缀配置; 例子中的renren-fast的部分来自这里
```
server:
  servlet:
    context-path: /renren-fast
```
- 4.然后发现前端还是没法访问后端, 此时有跨域问题; 使用该类解决 GulimallCorsConfiguration

#### 跨域问题
- 跨域问题: 指的是浏览器不能执行其他网站的脚本, 他是浏览器的同源策略造成的, 是浏览器对javaScript施加安全的限制
- 同源策略: 是指协议, 域名, 端口都要相同, 其中有一个不同都会产生跨域
- 跨源资源共享(CORS): https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS
- 非简单请求的跨域流程: 预检请求OPTIONS; 如果不允许则不会再发真实数据
    - 浏览器 --- 1. 预检请求    --> 服务器
    - 浏览器 <-- 2. 响应允许跨域 --- 服务器
    - 浏览器 --- 3. 发送真实数据 --> 服务器
    - 浏览器 <-- 4. 响应数据    --- 服务器
- 解决方案:
    - 使用nginx部署为同一个域
    - 配置当次请求允许跨域
- 跨域报错: 后端收到请求并且成功返回, 只不过浏览器把结果返回拦截并报错, 满足同源策略浏览器才能读到服务端的响应
- spring支持跨域的各种配置就是根据request往response中增加header; 一般在网关中配置这些跨域请求
    
#### 启动失败
- springBoot启动失败多半是因为配置文件没有配置好造成的
- 数据库配置的地址连接不上mybatis出问题造成项目启动失败
- 好好检查下yaml配置文件的格式, 对齐空格之类的, 层级之间的缩进
- 如果@Value获取不到值, 多半是@Value中${}内部的路径不对, 或者单词写错了
- 无需配置数据源@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)

#### windows/Linux端口占用
- 查看8080端口被哪个进程PID占用
```
netstat -aon | findstr 8080
netstat -tunpl | grep 8080
``` 
- 查看当前进程PID是哪个程序
```
tasklist | findstr 143232
ps -ef | grep 143232
```
- 关闭该进程
```
taskkill /T /F /PID 143232
kill -9 143232
```

#### 数据库突然连接不上去了
- 虚拟机中的ip地址与无线网的ip地址冲突了, 修改下虚拟机中的ip地址即可
- 修改完成ip地址都互相ping通过的情况下, 发现还是连接不上数据库, 关闭下防火墙(虽然之前没关闭也能用--玄学)
- 达到最大连接数量了, 修改数据库配置my.cnf; `max_connections=1000`

#### 网站突然访问不了
- 由于无线网的ip地址一直变化, 需要重新重启这些微服务
- 重新配置nginx的网关ip地址, 配置成最新的ip地址
- 有时候还会和虚拟机的ip冲突, 这就比较麻烦了需要重置虚拟机ip

#### redis的OutOfDirectMemoryError错误
- 原因: SpringBoot2.0以后默认使用lettuce作为操作redis的客户端, 使用netty进行通信
- netty通信具有极高的吞吐量, 但是lettuce没有及时释放堆外内存造成堆外内存溢出
- 解决: 升级lettuce客户端; 或切换使用jedis客户端
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

#### nginx遇到的问题
- nginx静态文件修改不生效: 清除浏览器缓存最有效; 网上查到的没一个有用的
- nginx的404 not found: 本地使用的网络ip地址一直变化, 需要确保nginx配置的网关ip是正确的

#### Request method 'POST' not supported -- 405
- 在该项目中 /regist 请求是POST请求, 注册出错后转发到注册页面; return "forward:/reg.html";
- 而reg.html是使用路径映射的方式做的, 路径映射默认都是GET方式访问的
- 转发就是原来的请求原封不动转给下个页面, 将一个POST请求转发给了GET请求所以产生这个问题
- 不使用转发直接渲染界面 return "reg";

#### feign远程调用丢失请求头
- 浏览器发送请求时请求头自动携带cookie, 而feign是一个崭新的请求
- feign远程调用的时候创建一个新的request, 无任何请求头
- 构造请求的时候会调用拦截器丰富feign请求内容添加上feign远程调用的请求拦截器
- 在拦截器中同步源请求头的数据, 主要是cookie; 参考GulimallFeignConfig类
```
Request targetRequest(RequestTemplate template) {
    for (RequestInterceptor interceptor : requestInterceptors) {
      interceptor.apply(template);
    }
    return target.apply(template);
  }
```

#### feign异步情况丢失上下文问题
- spring中的上下文信息是存储在RequestAttributes存储在ThreadLocal中的
- 因为是异步情况会开启多个线程但ThreadLocal只能在一个线程中使用, 所以上下文信息就没有了
- 在异步情况发送feign请求之前, 手动设置下上下文RequestContextHolder.setRequestAttributes(xxx);

#### RabbitMQ消息一直unack
- 在进行消息监听的时候出现异常, 消息从ready变为unack状态, 是因为传入对象与接受的对象不一致造成的
- 在手动确认的模式下, 消息的unack状态不会变为ready状态, 重启或者断开连接就能够从新回到ready状态了

#### 支付宝沙箱功能, 存在钓鱼风险提示页面
- 换一个浏览器就可以了, 或者清除支付宝相关网页的cookie即可

#### 异步通知无法访问订单服务
- 使用了内网穿透工具, 请求头host并不是order.gulimall.com, 而是外网地址http://cys-mall.natapp1.cc/
- conf.d文件夹添加代理配置gulimall.conf, 该文件夹下的所有配置文件都回包含在总配置文件中
```
server_name  gulimall.com  *.gulimall.com cys-mall.natapp1.cc;

location /payed/ {
    proxy_set_head Host order.gulimall.com;
    proxy_pass http://gulimall;
}
```

#### 远程主机强迫关闭了一个现有的连接
- 与某些服务失去连接, redis、mysql等中间件; 检查服务是否能够连接, 有可能是网络波动引起的(玄学)

### 规范
#### REST接口
- Controller处理请求, 接收和校验数据
- Service处理controller传来的数据, 进行业务处理
- Controller接收Service处理完的数据, 封装页面指定的VO

#### vo与to(本项目中)
- 不同应用进行传输的为vo, 前后端, 微服务等调用
- 用于中间件传输的为to, rabbitMq, redis等

#### 耗时
- http(请求微信api) > 内网 + 磁盘(mysql) > 内存
- 代码中最忌讳的就是在for循环中做http、sql
- 不要让数据库/数据存储中间件做任何业务操作