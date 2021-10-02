### 谷粒商城

![项目架构图](https://github.com/CyS2020/SpringCloud-Mall/blob/main/resources/%E8%B0%B7%E7%B2%92%E5%95%86%E5%9F%8E-%E5%BE%AE%E6%9C%8D%E5%8A%A1%E6%9E%B6%E6%9E%84%E5%9B%BE.jpg?raw=true)


#### 关于项目
- 关于项目中所需要的数据库创建代码均放在data/sql目录下
- renren_fast与renren-generator是从码云上人人开源clone下来的, 时间2021.08.13
- renren-fast-vue前端代码所需的nodeJs安装的是14.17.5版本, 前后端均可以启动成功

#### 逆向工程
- 使用renren-generator工具生成五个微服务的crud的代码: controller; dao; entity; service
- 配置pom文件解决基本的依赖问题, 配置yml文件启动项目, 测试接口是否正常运行

#### 整合Mybatis-plus依赖
- 导入依赖
```
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.2.0</version>
</dependency>
```
- 配置信息
    - 导入mysql驱动的依赖，放在gulimall-common模块了
    - 在application.yml配置mysql数据源相关信息
    - 配置mybatis-plus: 配置@MapperScan扫描的包路径(项目启动类)
    - 配置mybatis-plus: 配置classpath扫描的xml文件路径(配置文件)
- 逻辑删除
    - 配置全局的逻辑删除规则(省略)
    - 配置逻辑删除的组件Bean(省略)
    - 给Bean(数据库的entity对象)加上逻辑删除注解@TableLogic
- 分页插件
    - 给项目添加MybatisConfig分页功能的配置项

#### 微服务
- 注册中心: 每一个微服务上线后注册到注册中心，对外提供服务;
- 配置中心: 每一个微服务的配置都很多, 集群环境需要一个个修改, 通过配置中心来管理修改;
- 网关: 前端请求通过网关进行 鉴权; 过滤; 路由. 由网关抵达微服务;

#### 技术搭配方案
- SpringCloud Alibaba - Nacos：注册中心（服务发现/注册）
- SpringCloud Alibaba - Nacos：配置中心（动态配置管理）
- SpringCloud - Ribbon：负载均衡
- SpringCloud - Feign：声明式 HTTP 客户端（调用远程服务）
- SpringCloud Alibaba - Sentinel：服务容错（限流、降级、熔断）
- SpringCloud - Gateway：API 网关（webflux 编程模式）
- SpringCloud - Sleuth：调用链监控
- SpringCloud Alibaba - Seata：原 Fescar，即分布式事务解决方案

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
- 配置中心添加一个名叫 xxx.properties 的数据集，服务名.properties(默认规则)--配置中心的
- bootstrap.properties文件可以配置初始加载文件(配置中心的某个文件)
- java代码中配置类动态刷新@RefreshScope, 获取某个配置的值@Value("${配置项}")
- 如果配置中心与本地配置文件中的冲突, 优先使用配置中心的配置

#### Nacos配置中心细节
- 命名空间: 用于进行租户粒度的配置隔离. 不同的命名空间下，可以存在相同的Group或Data ID的配置; 默认public; 
开发, 测试, 生产中利用命名空间做环境隔离. 在bootstrap.properties配置命名空间的id
- 配置集: 一组相关或者不相关的配置项的集合称为配置集
- 配置集ID: 类似于以前的配置文件名application.yml
- 配置组: 默认所有的配置集都属于DEFAULT_GROUP组
- 使用细节: 每个微服务创建自己的命名空间; 使用配置分组来区分环境dev, test, prod;
- 同时加载多个配置集, 我们的任何配置文件都可以放在配置中心中, bootstrap.properties文件中配置需要加在的配置集
- @Value, @ConfigurationProperties...等从配置文件中获取值得使用方式仍然可用

#### OpenFeign远程调用
- 引入open-feign依赖
- 编写接口，告诉springCloud这个接口需要调用的那个微服务
- 声明方法，调用微服务的哪个请求，访问路径需要写全
    - 访问路径写法有两种一种是过网关(网关微服务 + /api/xx/xxx), 一种是不过网关(指定微服务 + /xx/xxx)
- 开启远程调用功能，@EnableFeignClients并传入扫描的包路径
- SpringCloud整个远程调用的逻辑
    - 如果有个Service调用了feign的Service, 并且传入了对象
    - 若有@RequestBody则将这个对象转为json
    - 去注册中心中找到该服务, 将json数据放在请求体的位置, 给对应的Rest接口发送请求
    - 对方服务收到请求以及请求体中的json数据
    - 将请求体中的json数据转为该接口接收对象
- 只要json数据模型是兼容的, 远程调用双方无需使用同一个TO
    

#### GateWay网关
- 创建一个模块作为项目的API网关微服务, 同时写需要引入注册中心与配置中心的功能
- 开启服务注册功能@EnableDiscoveryClient(可省略)
- 使用配置中心功能并进行配置与其他项目一样
- 启动失败是因为公共组件有Mybatis-plus因此需要配置数据源, 可以再启动项目的地方exclude掉

#### GateWay技术细节
- 功能：路由, 鉴权, 限流
- Route: 网关的基本构建块即路由规则. 它由ID, 目标URI, 谓词集合和过滤器集合定义, 如果聚合谓词为真, 则匹配路由
- Predicate: 这是一个Java 8函数谓词，这使您可以匹配来自HTTP请求的任何内容, 例如标头或参数
- Filter: 这些是GatewayFilter使用特定工厂构建的实例. 在这里您可以在发送下游请求之前或之后修改请求和响应
- Predicate如果满足某种规则才进行路由，Filter对谓词中的内容进行判断分析处理不是狭义的过滤, 可以是增删改操作

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
- RestControllerAdvice会自动帮助catch,并匹配相应的ExceptionHandler, 然后重新封装异常信息, 返回值, 统一格式返回给前端。

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
  - 一个 tokenizer(分词器)接收一个字符流, 将之分割为独立的tokens(词元，通常是独立的单词)，然后输出tokens流
  - 使用中文分词器: 安装插件elasticsearch-analysis-ik
  - 自定义词库: 使用nginx服务器来存储自定义的字典, 然后修改/usr/share/elasticsearch/plugins/ik/config/中的 IKAnalyzer.cfg.xml中配置远程扩展字典地址
  
#### ElasticSearch客户端
  - 在docker容器中安装ElasticSearch服务并启动
  - 引入客户端依赖elasticsearch.client
  - 编写配置类能够访问远程的ElasticSearch服务器并向容器中注入
  - 参照API对ElasticSearch进行操作
  - `https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high.html`
  
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
  - 就是从现实世界中抽象出来的有形或无形的业务实体。
- TO(Transfer Object)--数据传输对象
  - 不同应用程序之间传输的对象
- DTO(Data Transfer Object)--数据传输对象
  - 泛指用于展示层与服务层之间的数据传输对象
- VO(View Object)--视图对象
  - 接受页面传递来的数据封装对象; 将业务处理完成的对象封装成页面要用的数据
- BO(business Object)--业务对象
  - 主要作用是把业务逻辑封装为一个对象。这个对象可以包括一个或多个其它的对象
- POJO(Plain Ordinary Java Object)--简单无规则java对象
  - 我的理解就是最基本的java Bean, 只有属性字段及setter和getter方法, POJO是DO/DTO/BO/VO的统称
- DAO(Data Access Object)--数据访问对象
  - DAO中包含了各种数据库的操作方法, 通过它的方法结合PO对数据库进行相关的操作, 夹在业务逻辑与数据库资源中间配合VO, 提供数据库的CRUD操作

#### 数据库表
- 三级分类 -> spu -> sku -> 属性分组 -> 具体属性 -> 具体属性的值
- 属性与属性分组表会记录自己的三级分类信息; 属性与属性分组有关联关系表; 品牌与分类有关联关系表


### 拦路虎
#### Nacos启动失败
- 修改startup.cmd文件，默认使用集群模式启动，可以将启动模式改为set MODE="standalone"

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
    
#### 启动失败
- springBoot启动失败多半是因为配置文件没有配置好造成的
- 数据库配置的地址连接不上mybatis出问题造成项目启动失败
- 好好检查下yaml配置文件的格式, 对齐空格之类的, 层级之间的缩进
- 如果@Value获取不到值, 多半是@Value中${}内部的路径不对, 或者单词写错了
- 无需配置数据源@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)

#### 数据库突然连接不上去了
- 虚拟机中的ip地址与无线网的ip地址冲突了, 修改下虚拟机中的ip地址即可
- 修改完成ip地址都互相ping通过的情况下, 发现还是连接不上数据库, 关闭下防火墙(虽然之前没关闭也能用--玄学)

### 规范
#### REST接口
- Controller处理请求, 接收和校验数据
- Service处理controller传来的数据, 进行业务处理
- Controller接收Service处理完的数据, 封装页面指定的VO

#### 耗时
- http(请求微信api) > 内网 + 磁盘(mysql) > 内存
- 代码中最忌讳的就是在for循环中做http、sql