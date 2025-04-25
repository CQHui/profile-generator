# Profile Generator

基于Spring Boot和AI的简历处理系统，能够将简历转换为结构化文本、YAML配置和个人主页。

## 功能特点

- **简历解析**：上传PDF/文本简历，提取结构化文本
- **配置生成**：将简历转换为中英双语YAML配置
- **个人主页生成**：基于配置自动生成美观的个人主页HTML文件
- **OSS存储支持**：将生成的文件存储在云端供下载和分享
- **多语言支持**：自动生成中文和英文两个版本的个人资料

## 技术栈

- **后端**：Java 17+, Spring Boot 3.x
- **AI集成**：Spring AI (支持多种大语言模型)
- **文件处理**：Apache Tika, OSS文件存储
- **前端**：HTML/CSS/JavaScript, 响应式设计
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

## API接口

1. **转换简历为文本**
   - `POST /api/resume/convert`
   - 请求参数: `file` (简历文件)

2. **生成YAML配置**
   - `POST /api/resume/generate-config`
   - 请求参数: `file` (简历文件)

3. **生成个人主页**
   - `POST /api/resume/generate-profile`
   - 请求参数: `key` (唯一标识), `file` (PDF简历)

## 快速开始

### 环境要求

- JDK 17或更高版本
- Maven 3.6+
- Node.js (用于HTML生成)
- 阿里云OSS访问密钥 (可选，用于远程存储)

### 配置

在`application.yml`中配置以下内容：

```yaml
spring:
  ai:
    # 配置您的AI模型
    dashscope:
      api-key: 您的API密钥

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
git clone https://github.com/yourusername/profile-generator.git
cd profile-generator
```

2. 安装依赖
```bash
mvn clean install
cd src/main/resources/static/profile_website
npm install
cd ../../../../../..
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

1. **转换简历文本**：
   - 点击"基础功能"，上传简历，点击"转换为文本"
   - 查看解析结果

2. **生成YAML配置**：
   - 点击"基础功能"，上传简历，点击"生成YAML配置"
   - 下载生成的配置文件

3. **生成个人主页**：
   - 切换到"生成个人主页"选项卡
   - 输入唯一标识符（例如您的名字）
   - 上传PDF简历
   - 点击"生成个人主页配置"
   - 查看结果并下载生成的HTML文件

## 自定义

### 修改HTML模板

如需修改生成的HTML页面样式和结构：
1. 编辑 `src/main/resources/static/profile_website/template.html`
2. 调整CSS样式或布局结构

### 修改AI提示

如需调整AI生成内容的格式和指导：
1. 修改 `ResumeProcessingService.java` 中的提示模板
2. 根据需求调整YAML结构或内容格式

## 贡献指南

欢迎提交问题和拉取请求。贡献前请先讨论您想进行的更改。

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件 