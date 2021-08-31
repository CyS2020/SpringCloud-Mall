### 谷粒商城
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
    - 配置mybatis-plus: 配置@MapperScan扫描的包路径
    - 配置mybatis-plus: 配置classpath扫描的xml文件路径
- 逻辑删除
    - 配置全局的逻辑删除规则(省略)
    - 配置逻辑删除的组件Bean(省略)
    - 给Bean(数据库的entity对象)加上逻辑删除注解@TableLogic

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
- 创建bootstrap.properties文件配置 配置中心的服务器地址; 以及当前服务的名称
- 配置中心添加一个名叫 gulimall-coupon.properties 的数据集，服务名.properties(默认规则)
- 给服务名.properties 添加任何配置
- 动态刷新配置@RefreshScope, 获取某个配置的值@Value("${配置项}")
- 如果配置中心与当前文件中的配置冲突, 优先使用配置中心的配置

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
- 开启远程调用功能，@EnableFeignClients并传入扫描的包路径

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
    