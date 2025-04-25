package com.qihui.profilegenerator.service;

import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ResumeProcessingService {
    // 系统提示词
    private final static String TO_TEXT_PROMPT = """
            你是一个简历解析专家。你的任务是将用户上传的简历内容（可能是文本格式或从PDF提取的文本）转换为结构化的纯文本格式。
            请保留原始简历中的所有重要信息，包括个人信息、教育背景、工作经验、技能、项目经验等。
            返回的内容应当是整洁、格式一致的纯文本，便于后续处理。
            请不要添加任何额外的解释或评论，只需返回转换后的文本内容。
            """;

    // 系统提示词
    private final static String TO_YAML_PROMPT = """
            你是一个专业的简历内容提取专家。你的任务是将上传的简历文本转换为两个YAML文件（中文zh.yaml和英文en.yaml）。
            
            【输出格式要求1】
            1. 仅输出YAML文件内容，不要输出任何解释、说明或其他额外文字
            2. 不要使用Markdown格式，不要添加```yaml、```等代码块标记
            3. 不要添加任何# zh.yaml 或 # en.yaml 这样的标题行
            4. 直接输出原始YAML内容，确保可以直接复制粘贴使用

            【格式要求2】
            1. 首先输出完整的中文YAML配置（zh.yaml）
            2. 然后输出一行分隔符"---"（三个连字符）
            3. 最后输出完整的英文YAML配置（en.yaml）
            
            【内容要求】
            - 保持与原始简历内容一致，不要添加简历中没有的信息
            - 如果简历中没有某个字段的信息，保留该字段为空值
            - 对于basic.roles字段，根据简历内容推断出适当的角色标签（2-6个）
            - 对于about.description字段，根据简历内容生成一段个人概述
            - 对于projects.description字段，尽可能详细还原原文本中的项目描述
            - basic.resumeLink字段必须使用以下值: {resumeLink}
            
            【YAML结构】
            # 基本信息
            basic:
              name: "亚历克斯·摩根"
              title: "全栈开发工程师 & UI/UX设计师"
              location: "旧金山，加利福尼亚"
              email: "alex.morgan@example.com"
              github: "https://github.com/alexmorgan"
              linkedin: "https://linkedin.com/in/alexmorgan"
              phone: "+1 (415) 555-1234"
              resumeLink: "resume/Alex-Morgan-Resume-CN.pdf"
              roles:
                - "全栈开发工程师"
                - "用户体验设计师"
                - "问题解决专家"
                - "技术爱好者"
                        
            # 关于我
            about:
              description: "我是一名热情的全栈开发者，拥有超过8年的现代Web应用程序开发经验。我专注于React、Node.js和云架构，创建能够解决实际业务问题的可扩展解决方案。我信奉简洁代码、以用户为中心的设计和持续学习。在不编程的时候，你可以看到我在山间徒步或尝试新的烹饪食谱。"
              skills:
                - "JavaScript/TypeScript"
                - "React/Next.js"
                - "Node.js/Express"
                - "GraphQL/REST API"
                - "AWS/Azure"
                - "MongoDB/PostgreSQL"
                - "Docker/Kubernetes"
                - "CI/CD流水线"
                - "UI/UX设计"
                - "响应式设计"
                - "测试 (Jest, Cypress)"
                - "性能优化"
                        
            # 工作经历
            experience:
              - company: "科技创新解决方案"
                position: "高级全栈开发工程师"
                period: "2020 - 至今"
                logo: "https://upload.wikimedia.org/wikipedia/commons/c/cf/Cisco_logo-1000px.png"
                description:
                  - "领导5人开发团队重建公司旗舰SaaS平台，用户参与度提高45%"
                  - "设计并实现基于微服务的后端系统，使用Node.js、Express和MongoDB，系统可扩展性提高300%"
                  - "设计并开发响应式React前端，使用Redux进行状态管理，提升各种设备上的用户体验"
                  - "实施CI/CD流水线，使用GitHub Actions，减少70%的部署时间并提高代码质量"
                  - "优化应用性能，页面加载时间减少60%，达到99%的Lighthouse评分"
                        
              - company: "数字前沿公司"
                position: "前端开发工程师"
                period: "2017 - 2020"
                logo: "https://upload.wikimedia.org/wikipedia/commons/c/cf/Cisco_logo-1000px.png"
                description:
                  - "开发和维护多个基于React的Web应用，服务超过50,000日活用户"
                  - "与UX设计师合作实现响应式、无障碍的界面，遵循WCAG准则"
                  - "构建可重用组件库并在项目间建立前端编码标准"
                  - "将RESTful API和GraphQL端点与前端应用集成，提供实时数据"
                  - "指导初级开发人员并进行代码审查，确保高代码质量"
                        
              - company: "网络工艺工作室"
                position: "初级Web开发人员"
                period: "2015 - 2017"
                logo: ""
                description:
                  - "为各行业客户构建定制网站和Web应用程序"
                  - "使用HTML5、CSS3和JavaScript/jQuery实现响应式设计"
                  - "创建并集成WordPress主题和插件用于内容管理系统"
                  - "与设计团队合作将设计模型转化为功能性网站"
                  - "通过图像压缩、代码压缩和缓存策略优化网站性能"
                        
            # 项目经历
            projects:
              - name: "生态追踪"
                description: "一个可持续发展追踪平台，帮助用户通过日常习惯监控和减少碳足迹。功能包括个性化建议、进度追踪和社区挑战。"
                technologies: ["React", "Node.js", "MongoDB", "Chart.js", "AWS"]
                period: "2021.03 - 2022.08"
                responsibilities:
                  - "作为项目负责人设计并实现了整个应用架构，包括前端界面和后端API"
                  - "开发基于机器学习的碳足迹计算算法，提高计算准确率达40%"
                  - "实现实时数据可视化仪表板，帮助用户直观了解其环保进度"
                  - "带领3人团队完成了从概念到发布的全过程，按时交付所有里程碑"
                  - "通过A/B测试优化用户体验，使用户留存率提高28%"
                link: "https://ecotrack-demo.herokuapp.com"
                github: "https://github.com/alexmorgan/ecotrack"
               \s
              - name: "任务流专业版"
                description: "一个具有高级任务管理功能的生产力应用，包括看板、时间追踪和团队协作工具。与Google日历和Slack等流行服务集成。"
                technologies: ["React", "Redux", "Express", "PostgreSQL", "Socket.io"]
                period: "2019.06 - 2021.02"
                responsibilities:
                  - "设计并实现了可拖拽的看板界面，提升任务组织效率达35%"
                  - "开发RESTful API和实时通知系统，支持多用户同时协作"
                  - "集成第三方服务API（Google日历、Slack、Trello），扩展应用功能"
                  - "构建自动化测试套件，覆盖率达85%，大幅减少生产环境bug"
                  - "实现数据导出和分析模块，帮助用户获取工作效率洞察"
                link: "https://taskflow-pro.netlify.app"
                github: "https://github.com/alexmorgan/taskflow-pro"
               \s
              - name: "健康脉搏"
                description: "一个医疗分析仪表板，供患者和医疗提供者可视化健康指标变化。实现安全认证和符合HIPAA的数据存储。"
                technologies: ["Vue.js", "Firebase", "D3.js", "TailwindCSS"]
                period: "2018.09 - 2019.05"
                responsibilities:
                  - "设计并实现符合HIPAA标准的安全数据存储和传输架构"
                  - "使用D3.js开发交互式数据可视化图表，提高医疗数据理解率达50%"
                  - "实现基于角色的访问控制系统，严格区分患者和医疗提供者权限"
                  - "优化移动端体验，使应用在所有设备上都保持高性能和易用性"
                  - "与医疗专业人员合作验证功能需求，确保应用满足临床实践需求"
                link: "https://health-pulse.web.app"
                github: "https://github.com/alexmorgan/health-pulse"
                        
            # 教育经历
            education:
              - school: "加州大学伯克利分校"
                degree: "理学硕士"
                period: "2013 - 2015"
                major: "计算机科学"
                description:
                  - "获得计算机科学与工程学院杰出学生奖学金"
                  - "参与人工智能实验室研究项目，专注于机器学习算法优化"
                  - "完成高级数据结构与算法、分布式系统和机器学习课程"
                  - "担任本科生算法课程的助教，负责编写教材和指导学生项目"
               \s
              - school: "斯坦福大学"
                degree: "理学学士"
                period: "2009 - 2013"
                major: "软件工程"
                description:
                  - "主修软件工程，辅修人工智能，GPA 3.9/4.0"
                  - "参与学生开发团队，为校园开发移动应用程序"
                  - "获得Dean's List荣誉，连续三年"
                  - "参加校内黑客马拉松比赛并获得最佳创新奖"
                        
            # 证书
            certifications:
              - name: "AWS认证解决方案架构师"
                issuer: "亚马逊网络服务"
                date: "2021"
               \s
              - name: "Google专业云开发者"
                issuer: "Google Cloud"
                date: "2020"
               \s
              - name: "认证Kubernetes管理员"
                issuer: "云原生计算基金会"
                date: "2019"
                        
            # 界面文本
            ui:
              nav:
                home: "首页"
                about: "关于"
                experience: "经历"
                projects: "项目"
                education: "教育"
                certifications: "证书"
                contact: "联系"
              hero:
                greeting: "你好，我是"
                intro: "我是一名"
                buttons:
                  contact: "联系我"
                  projects: "我的作品"
              about:
                title: "关于我"
                subtitle: "我是谁？"
                story: "我的故事"
                skills: "我的技能"
                button: "联系我"
              experience:
                title: "工作经历"
                subtitle: "我的专业旅程"
              projects:
                title: "作品集"
                subtitle: "近期项目"
                buttons:
                  view: "查看项目"
                  github: "GitHub"
              education:
                title: "教育背景"
                subtitle: "学术经历"
              certifications:
                title: "证书"
                subtitle: "专业发展"
              contact:
                title: "联系方式"
                subtitle: "与我联系"
                email: "邮箱"
                phone: "电话"
                location: "位置"
              footer:
                rights: "版权所有"
              lang:
                switch: "中文 / EN"
                resume: "简历"
            """;

    private final ChatClient chatClient;

    /**
     * 将用户上传的简历内容转换为纯文本格式
     * @param resumeContent 用户上传的简历内容（文本或从PDF提取的文本）
     * @return 转换后的纯文本格式简历
     */
    public String convertResumeToText(String resumeContent) {
        // 创建系统提示
        Message systemMessage = new SystemPromptTemplate(TO_TEXT_PROMPT)
                .createMessage(Map.of());
        
        // 创建用户消息
        Message userMessage = new UserMessage(resumeContent);
        
        // 创建提示并发送到AI模型
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        
        // 调用AI并获取响应
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 将用户上传的简历内容转换为YAML格式
     * @param resumeContent 用户上传的简历内容（文本或从PDF提取的文本）
     * @param resumeLink 指定的简历链接路径
     * @return 转换后的YAML格式简历
     */
    public Flux<String> convertResumeToYaml(String resumeContent, String resumeLink) {
        // 创建系统提示，并传入resumeLink参数
        Message systemMessage = new SystemPromptTemplate(TO_YAML_PROMPT)
                .createMessage(Map.of("resumeLink", resumeLink != null ? resumeLink : ""));

        // 创建用户消息
        Message userMessage = new UserMessage(resumeContent);

        // 创建提示并发送到AI模型
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // 调用AI并获取响应
        return chatClient.prompt(prompt).stream().content();
    }
}
