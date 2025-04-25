# Profile Generator

基于Spring Boot和AI的简历处理系统，能够将简历转换为结构化文本、YAML配置和个人主页。

## 功能特点

- **简历分析**：上传PDF简历，自动提取结构化信息
- **双语配置生成**：自动生成中英双语YAML配置文件
- **个人主页生成**：基于配置自动创建美观的个人主页HTML文件
- **OSS云存储**：将生成的文件存储在云端供下载和分享
- **响应式设计**：生成的个人主页适配各种设备屏幕
- **AI增强**：利用大语言模型提取和扩展简历内容

## 技术栈

- **后端**：Java 17+, Spring Boot 3.2+
- **AI集成**：Spring AI (支持多种大语言模型)
- **文件处理**：Apache Tika, 阿里云OSS
- **前端**：HTML/CSS/JavaScript, Bootstrap, Font Awesome
- **构建工具**：Maven

## 系统架构

```
┌─────────────┐     ┌───────────────┐     ┌─────────────────┐
│ Web 界面    │────▶│ Spring Boot API│────▶│ AI 处理服务      │
└─────────────┘     └───────────────┘     └─────────────────┘
                           │                        │
                           ▼                        ▼
                    ┌───────────────┐     ┌─────────────────┐
                    │ OSS文件存储    │◀────│ YAML配置生成     │
                    └───────────────┘     └─────────────────┘
                           │                        │
                           │                        ▼
                           │              ┌─────────────────┐
                           └─────────────▶│ HTML页面生成     │
                                          └─────────────────┘
```

## 核心组件

1. **ResumeController**: 处理文件上传和生成请求
2. **ResumeProcessingService**: 利用AI模型处理简历内容
3. **OssFileProcessingService**: 处理文件上传和提取
4. **OssService**: 管理OSS云存储操作

## API接口

### 生成个人主页
- **端点**: `POST /api/resume/generate-profile`
- **参数**: 
  - `key` (唯一标识符，如用户名)
  - `file` (PDF格式简历)
- **响应**:
  ```json
  {
    "success": true,
    "content": "个人档案配置文件已生成并保存，可以通过以下链接下载HTML文件",
    "configFiles": [
      "/profile_website/config/zh.yaml",
      "/profile_website/config/en.yaml"
    ],
    "htmlUrl": "https://example.com/your-profile.html",
    "processingTimeMs": 3500
  }
  ```

## 快速开始

### 环境要求

- JDK 17或更高版本
- Maven 3.6+
- Node.js (用于HTML生成)
- 阿里云OSS访问密钥 (用于远程存储)

### 配置

在`application.yml`中配置以下内容：

```yaml
spring:
  ai:
    # 配置AI模型
    dashscope:
      api-key: 您的API密钥
    # 或者配置其他模型:
    # openai:
    #   api-key: 您的OpenAI密钥

aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com  
    access-key-id: 您的accessKeyId
    access-key-secret: 您的accessKeySecret
    bucket-name: 您的存储桶名称
    dir-prefix: profile-generator/
```

### 本地运行

1. 克隆项目
```bash
git clone https://github.com/username/profile-generator.git
cd profile-generator
```

2. 安装依赖
```bash
mvn clean install
```

3. 启动应用
```bash
mvn spring-boot:run
```

4. 访问Web界面
```
http://localhost:8080
```

## 使用方法

1. **访问首页**：打开浏览器访问应用首页
2. **输入标识符**：输入您的唯一标识符（如姓名或用户名）
3. **上传简历**：上传PDF格式的简历文件
4. **生成主页**：点击"生成个人主页"按钮
5. **下载资源**：生成完成后，您可以下载生成的HTML文件和查看配置

## 自定义设置

### 修改HTML模板

如需修改生成的HTML页面样式和结构：
1. 编辑 `src/main/resources/static/profile_website/template.html`
2. 调整CSS样式或布局结构

### 修改AI提示

如需调整AI生成内容的格式和指导：
1. 修改 `ResumeProcessingService.java` 中的提示模板
2. 根据需求调整YAML结构或内容格式

## 进阶开发

### 新增语言支持
要添加其他语言的支持，需要：
1. 修改 `TO_YAML_PROMPT` 提示词，增加目标语言的模板
2. 更新前端显示逻辑，添加语言切换功能

### 添加更多简历格式支持
要支持更多简历格式（如Word或HTML）：
1. 在 `OssFileProcessingService` 中添加相应格式的处理逻辑
2. 更新控制器中的文件类型验证

## 许可证

本项目采用 [MIT 许可证](./LICENSE)。 