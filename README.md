# common-abilities

`common-abilities` 是 `link.cu1universe.dev` 团队的基础 Maven 聚合项目，用于统一管理依赖版本和构建配置，减少各业务子项目中的重复配置。

## 项目结构

```
common-abilities
├── pom.xml                    # 根聚合 POM（packaging=pom）
├── common-dependencies
│   └── pom.xml                # BOM：统一管理第三方依赖版本
├── common-parent
│   └── pom.xml                # Parent POM：统一管理插件版本和构建行为
└── common-apollo
    └── pom.xml                # Apollo 通用扩展组件
```

根目录聚合全部模块，父 POM 继承关系如下（箭头表示父 POM → 子 POM）：

```text
common-abilities → common-dependencies → common-parent → common-apollo
```

`dependencyManagement` 沿父链继承：根 POM 的声明仍然有效，同一依赖在更近的子 POM 中声明时，以子 POM 为准。BOM 管依赖版本，`common-parent` 管构建约定，Apollo 继承两者并保留库模块所需的局部配置。

### 模块说明

#### 1. `common-dependencies`
- **作用**：作为 **BOM（Bill of Materials）**，集中锁定所有第三方依赖的版本号。
- **使用方式**：业务项目通过 `<dependencyManagement>` 引入该 BOM 后，直接添加依赖无需再写 `<version>`。
- **覆盖范围**：Spring Boot、Spring Cloud、Spring Cloud Alibaba、MyBatis-Plus、Redisson、Jackson、Hutool、Apollo、COS/S3 SDK、Swagger/SpringDoc 等。

#### 2. `common-parent`
- **作用**：作为业务项目的 **Parent POM**，直接继承 `common-dependencies`，获得 BOM 的依赖版本和属性，进一步定义：
  - 编译参数（JDK 17、UTF-8）
  - 插件版本（Compiler、Surefire、Source、Jar、Deploy、Resources 等）
  - 打包行为（Spring Boot Repackage、Maven Shade、Git Commit ID）
  - 资源过滤规则（`application*.yml/yaml/properties`）
- **使用方式**：业务项目直接 `<parent>` 指向 `common-parent`。

#### 3. `common-apollo`
- **作用**：Apollo 的通用 Spring Boot 扩展模块。当前提供 `@ApolloStaticValue`，用于将 Apollo 配置绑定到 Spring Bean 中的 `static` 字段，并在配置变更后自动刷新。
- **Namespace**：通过 `apollo.bootstrap.namespaces` 监听已加载的 Namespace；业务代码只需要声明配置 key，不需要感知 Namespace。
- **构建**：继承 `common-parent`，使用 Java 17 编译，运行环境需 Java 17 或更高版本。保留宿主框架依赖的 `provided` 作用域，产物为普通库 JAR，不启用 Spring Boot Repackage 或 Shade。Apollo 不需要 Lombok/MapStruct 注解处理器，并显式禁止忽略测试失败。

#### Apollo 消费者验证

以下组合已在 JDK 17 上以独立消费者验证（消费者使用自己的 Spring Boot BOM，不继承本仓库父 POM）：

| Spring Boot | Apollo Client | 验证结果 |
|---|---|---|
| 2.6.6 | 2.1.0 | 自动配置发现、初始化、静态字段刷新通过 |
| 3.5.16 | 2.1.0 | 自动配置发现、初始化、静态字段刷新通过 |
| 4.1.1 | 2.1.0 | 自动配置发现、初始化、静态字段刷新通过 |

验证使用实际打包的 JAR 和完整父 POM 链，通过内存 Apollo 配置模拟变更，不连接真实 Apollo 服务；不代表所有中间版本或所有 Apollo 功能均已验证。消费者需自行提供 Apollo Client 和 Spring Boot 等运行时依赖；库的父 POM 管理配置不会替代消费者自己的依赖管理。

## 快速开始

### 1. 引入 BOM（仅管理版本，不传递依赖）

在业务项目的 `pom.xml` 中添加：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>link.cu1universe.dev</groupId>
            <artifactId>common-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

之后即可省略版本号：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 2. 继承 Parent POM（推荐）

在业务项目的 `pom.xml` 中直接继承：

```xml
<parent>
    <groupId>link.cu1universe.dev</groupId>
    <artifactId>common-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>
</parent>
```

这样可同时获得 **版本管理** 和 **统一构建配置**。

### 3. 使用 Apollo 静态配置热更新

业务项目引入本模块：

```xml
<dependency>
    <groupId>link.cu1universe.dev</groupId>
    <artifactId>common-apollo</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

将需要热更新的字段声明在 Spring Bean 中：

```java
import link.cu1universe.dev.apollo.annotation.ApolloStaticValue;

@Component
public class AppConfig {

    @ApolloStaticValue("${app.transfer-url:http://localhost:8080}")
    public static String transferUrl;

    @ApolloStaticValue("${movie.api.timeout:5000}")
    public static int movieTimeout;
}
```

配置 `apollo.bootstrap.namespaces=application,movie-shared` 后，上述字段会从 Spring `Environment` 读取初始值，并监听这两个 Namespace 的配置变化。注解仅支持非 `final` 的 `static` 字段，且每个注解必须只包含一个 Spring 占位符。

## 核心技术栈版本

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 2.6.6 |
| Spring Cloud | 2021.0.0 |
| Spring Cloud Alibaba | 2021.0.4.0 |
| MyBatis-Plus | 3.5.2 |
| Redisson | 3.11.6 |
| Jackson | 2.15.2 |
| Lombok | 1.18.28 |
| MapStruct | 1.5.3.Final |
| Hutool | 5.8.20 |
| Apollo Client | 2.1.0 |

## 构建

```bash
# 安装到本地 Maven 仓库
mvn clean install

# 跳过测试
mvn clean install -DskipTests
```

### 按子模块发布 Apollo 快照

在根目录执行：

```bash
# 构建并测试 Apollo 及其父链，不上传
mvn -pl common-apollo -am clean verify

# 仅上传 Apollo 子模块，仓库凭据使用 settings.xml 中的 central
mvn -pl common-apollo clean deploy
```

Apollo 快照发布地址为 `https://central.sonatype.com/repository/maven-snapshots/`，根 POM 不配置发布地址。**仅上传 Apollo 不代表消费者只需 Apollo 的 POM：`common-parent`、`common-dependencies` 和 `common-abilities` 的对应版本也必须先发布到消费者可访问的仓库**，本地 `install` 不能替代远程发布。首次使用完整继承链时，需要先准备这三个父 POM；后续它们版本不变时可以仅发布 Apollo。

## 注意事项

1. **版本号对齐**：根模块及子模块版本统一为 `1.0.0-SNAPSHOT`，调整版本时需同步更新父 POM 引用。
2. **发布策略**：该项目属于基础设施，建议通过 CI/CD 发布到公司私有 Maven 仓库（如 Nexus），业务项目引用时不要再使用 `-SNAPSHOT`。
3. **Guava 版本属性拼写**：根 `pom.xml` 中 `guava.verison` 应为 `guava.version`。
4. **单测框架**：已统一使用 **Spock + Groovy** 替换 JUnit 4，业务项目可直接引入 `spock-core` / `spock-spring` 进行单测。
