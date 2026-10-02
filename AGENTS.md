# 仓库指南

## 项目结构与模块职责

这是一个基于 Java 17 和 Maven 的多模块仓库，发布共享 Maven POM 及可供 Spring Boot 项目使用的基础组件。

- `pom.xml`：根聚合 POM，维护共享依赖版本和默认构建插件。
- `common-dependencies/pom.xml`：依赖 BOM。在此集中新增或更新受管依赖版本。
- `common-parent/pom.xml`：可复用父 POM，继承 `common-dependencies` 并集中管理插件与资源过滤配置。
- `common-apollo/`：继承 `common-parent` 的 Apollo 配置增强组件，提供静态字段配置刷新与自动配置，使用 Java 17 编译并按子模块发布。
- `README.md` 和 `LICENSE`：仓库级文档与许可文件。

依赖版本必须集中维护在相应 POM 的 `properties` 区块。消费者通过导入 `common-dependencies` 获取版本管理，或继承 `common-parent` 获取构建约定。

## 基础组件跨版本兼容性

面向框架消费者发布的基础组件，应在不牺牲明确功能边界的前提下，尽可能兼容当前及后续主流框架版本。具体支持版本以组件声明的兼容矩阵为准；实现时遵循以下规则。

- **隔离宿主依赖**：组件编译所需、但应由消费者应用在运行时提供的框架依赖，使用 Maven `provided` 作用域。不要将某个宿主框架版本及其传递依赖带入消费者运行时类路径，也不要在组件中强制覆盖消费者的依赖版本。
- **依赖稳定抽象**：优先使用目标框架长期稳定、跨大版本保留的 API、SPI 和生命周期接口；避免将实现绑定到可能重命名、迁移、废弃或删除的具体类型、注解、配置键和资源注册机制。
- **规避已知迁移断点**：遇到命名空间迁移、包重构、注解替换或配置模型变化时，避免直接依赖迁移前后的专有 API，改用稳定替代方案或增加隔离层。`javax.annotation.*` 与 `jakarta.annotation.*` 的迁移是典型示例：初始化逻辑可使用 Spring 的 `InitializingBean#afterPropertiesSet()`，而非依赖 `@PostConstruct`。
- **兼容性适配**：必须使用版本特定机制时，将差异封装在最小适配层中，保持公共 API 和核心业务逻辑独立；例如自动配置需按所支持版本提供相应的注册资源。
- **持续验证**：每次涉及框架依赖、自动配置、生命周期或公共 API 的改动，都要执行编译和测试，并在兼容矩阵覆盖的最低版本、当前版本及可用的更高版本消费者中进行集成验证。

## 构建、测试与开发命令

在仓库根目录、安装 Maven 和 JDK 17 后执行：

```bash
mvn clean verify
mvn test
mvn install
mvn -pl common-dependencies -am verify
mvn -pl common-apollo -am test
```

`clean verify` 会编译全部模块并运行完整生命周期；`test` 运行单元测试；`install` 还会将 POM 和组件产物安装到本地 Maven 仓库。最后两条命令分别校验依赖 BOM 及 Apollo 组件和其上游模块。仓库未提供 Maven Wrapper，也没有需要启动的本地应用。

## 代码风格与命名

POM 使用四个空格缩进，并遵循 Maven 常规的小写连字符 XML 元素命名。按库或生态系统归类属性与依赖。可复用版本使用 `${property.name}`，除非版本确实仅适用于单个模块，否则不要内联版本号。保留 UTF-8 编码和既有 XML 格式。

Java 代码遵循现有项目风格：包名全小写，类名使用 PascalCase，方法和字段使用 camelCase。新增公共 API 时，避免暴露仅在单一框架版本中存在的类型。

## 测试规范

POM 变更至少执行相应模块的 Maven 校验，确认依赖能够解析。`common-apollo` 包含 JUnit 测试；修改其实现、自动配置或依赖时，执行 `mvn -pl common-apollo -am test`。若受限环境无法写入 `~/.m2`，使用可写的临时本地仓库，例如 `-Dmaven.repo.local=/private/tmp/<task>-m2`，验证完成后清理该目录。

共享配置会管理 Maven Surefire 及供消费者使用的 Spock/Groovy 测试依赖；根构建配置可能忽略测试失败，因此除 Maven 退出码外，还应检查测试报告。

## 提交与合并请求规范

近期提交使用简洁的 Conventional Commit 风格主题，可采用中文范围描述，例如 `refactor(parent, dependencies): 重构公共依赖 bom 结构`。使用 `type(scope): summary`，范围可为 `dependencies`、`parent` 或 `apollo`。每个提交只包含一项连贯的版本或构建配置变更。

合并请求应说明受影响模块，列出升级或新增的依赖，解释兼容性影响，关联相关 issue，并附上执行过的 Maven 命令与结果。涉及基础组件时，还应明确说明覆盖的兼容版本及验证结论。
