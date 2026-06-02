# 智能图书借阅管理系统

这是一个符合 JavaEE 期末大作业要求的 Spring Boot 3 第一版项目，业务场景为图书借阅管理，并加入大模型图书推荐助手作为创新加分功能。

## 技术栈

- Spring Boot 3.2.12
- Spring Security
- Spring JDBC
- MySQL 8
- HTML/CSS/JavaScript
- OpenAI 兼容大模型 API

## 功能清单

- 登录认证：管理员、读者两种角色
- 图书管理：图书列表、搜索、新增、编辑、删除
- 分类管理：分类列表、新增、编辑、删除
- 借阅管理：借书、还书、我的借阅、管理员查看全部借阅
- 数据库设计：`users`、`roles`、`user_roles`、`categories`、`books`、`borrow_records`
- 创新功能：AI 图书推荐助手，支持接入 OpenAI 兼容接口

## 默认账号

- 管理员：`admin` / `admin123`
- 读者：`reader` / `reader123`

## 本地运行

1. 创建数据库：

```sql
CREATE DATABASE smart_library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改数据库连接，或设置环境变量：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/smart_library?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
```

3. 可选：配置大模型 API：

```powershell
$env:LLM_BASE_URL="https://api.openai.com"
$env:LLM_API_KEY="你的API Key"
$env:LLM_MODEL="gpt-4o-mini"
```

如果不配置 `LLM_API_KEY`，AI 助手会使用本地规则推荐，仍然可以演示功能入口和业务流程。

4. 启动项目：

```powershell
mvn spring-boot:run
```

5. 访问：

```text
http://localhost:8080
```

## 项目结构

```text
src/main/java/com/example/smartlibrary
├── config       安全配置、大模型配置、默认账号初始化
├── controller   页面路由和 REST 接口
├── dto          请求参数对象
├── exception    业务异常和统一异常处理
├── model        业务模型
├── repository   JDBC 数据访问
└── service      登录、借阅、大模型业务逻辑
```

## 大模型接口说明

接口地址：

```text
POST /api/ai/chat
```

请求示例：

```json
{
  "question": "请推荐两本适合学习 Spring Boot 的书"
}
```

系统会把馆藏数据作为上下文传给大模型，让回答与当前数据库中的图书相关。
