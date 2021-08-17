### 谷粒商城
#### 关于项目
- 关于项目中所需要的数据库创建代码均放在data/sql目录下
- renren_fast与renren-generator是从码云上人人开源clone下来的, 时间2021.08.13
- renren-fast-vue前端代码所需的nodeJs安装的是14.17.5版本, 前后端均可以启动成功

#### 逆向工程
- 使用renren-generator工具生成五个微服务的crud的代码: controller; dao; entity; service
- 配置pom文件解决基本的依赖问题, 配置yml文件启动项目, 测试接口是否正常运行

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