# common-abilities

`common-abilities` 是 `cn.cu1universe` 团队的基础 Maven 聚合项目，用于统一管理依赖版本和构建配置，减少各业务子项目中的重复配置。

## 项目结构

```
common-abilities
├── pom.xml                    # 根聚合 POM（packaging=pom）
├── common-dependencies
│   └── pom.xml                # BOM：统一管理第三方依赖版本
└── common-parent
    └── pom.xml                # Parent POM：统一管理插件版本和构建行为
```

### 模块说明

#### 1. `common-dependencies`
- **作用**：作为 **BOM（Bill of Materials）**，集中锁定所有第三方依赖的版本号。
- **使用方式**：业务项目通过 `<dependencyManagement>` 引入该 BOM 后，直接添加依赖无需再写 `<version>`。
- **覆盖范围**：Spring Boot、Spring Cloud、Spring Cloud Alibaba、MyBatis-Plus、Redisson、Jackson、Hutool、Apollo、COS/S3 SDK、Swagger/SpringDoc 等。

#### 2. `common-parent`
- **作用**：作为业务项目的 **Parent POM**，继承自根 `common-abilities`，并通过 `import` 引入 `common-dependencies` 的 BOM，进一步定义：
  - 编译参数（JDK 17、UTF-8）
  - 插件版本（Compiler、Surefire、Source、Jar、Deploy、Resources 等）
  - 打包行为（Spring Boot Repackage、Maven Shade、Git Commit ID）
  - 资源过滤规则（`application*.yml/yaml/properties`）
- **使用方式**：业务项目直接 `<parent>` 指向 `common-parent`。

## 快速开始

### 1. 引入 BOM（仅管理版本，不传递依赖）

在业务项目的 `pom.xml` 中添加：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.cu1universe</groupId>
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
    <groupId>cn.cu1universe</groupId>
    <artifactId>common-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>
</parent>
```

这样可同时获得 **版本管理** 和 **统一构建配置**。

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

## 注意事项

1. **版本号对齐**：根模块版本为 `1.0-SNAPSHOT`，子模块版本为 `1.0.0-SNAPSHOT`，建议统一。
2. **发布策略**：该项目属于基础设施，建议通过 CI/CD 发布到公司私有 Maven 仓库（如 Nexus），业务项目引用时不要再使用 `-SNAPSHOT`。
3. **Guava 版本属性拼写**：根 `pom.xml` 中 `guava.verison` 应为 `guava.version`。
4. **单测框架**：已统一使用 **Spock + Groovy** 替换 JUnit 4，业务项目可直接引入 `spock-core` / `spock-spring` 进行单测。
