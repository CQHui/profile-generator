package com.qihui.profilegenerator.controller;

import com.qihui.profilegenerator.dto.ResumeResponse;
import com.qihui.profilegenerator.service.OssFileProcessingService;
import com.qihui.profilegenerator.service.OssService;
import com.qihui.profilegenerator.service.ResumeProcessingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/resume")
@AllArgsConstructor
@Slf4j
public class ResumeController {

    private final ResumeProcessingService resumeProcessingService;
    private final OssFileProcessingService ossFileProcessingService;
    private final OssService ossService;
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * 根据指定的key生成个人资料配置
     * 将PDF上传到OSS的key目录，解析成文本，转换成YAML格式并保存
     *
     * @param key  用户指定的唯一标识
     * @param file 上传的简历文件（PDF格式）
     * @return 处理结果
     */
    @PostMapping("/generate-profile")
    public ResponseEntity<ResumeResponse> generateProfile(
            @RequestParam("key") String key,
            @RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();

        // 验证key格式
        if (!isValidKey(key)) {
            return ResponseEntity.badRequest().body(
                    ResumeResponse.error("无效的key格式，只允许字母、数字、下划线和连字符"));
        }

        try {
            // 记录上传文件信息
            logFileInfo(file);
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (!contentType.contains("application/pdf")) {
                return ResponseEntity.badRequest().body(
                        ResumeResponse.error("只支持PDF文件格式"));
            }
            
            // 构建OSS路径
            String ossPath = key + "/" + key + ".pdf";
            log.info("上传文件到OSS路径: {}", ossPath);
            
            // 使用OSS文件处理服务处理PDF文件
            String extractedText = ossFileProcessingService.processPdfFile(file, ossPath);
            log.info("成功处理PDF文件，提取文本长度: {} 字符", extractedText.length());
            
            // 调用AI服务生成YAML格式
            Flux<String> yamlFlux = resumeProcessingService.convertResumeToYaml(extractedText, ossService.getUrlPrefix() + ossPath);
            
            // 收集Flux内容为字符串
            String yamlContent = yamlFlux.collectList().map(chunks -> String.join("", chunks)).block();
            log.info("成功生成YAML内容，长度: {} 字符", yamlContent.length());
            
            // 保存YAML文件到指定目录
            String[] configFiles = saveYamlFilesWithKey(yamlContent, key);
            
            // 使用Node.js生成独立HTML文件
            String htmlFilePath = generateStandaloneHtml(key);
            
            // 上传HTML文件到OSS
            String htmlOssPath = key + "/" + key + ".html";
            ossFileProcessingService.uploadFile(new File(htmlFilePath), htmlOssPath);
            log.info("成功上传HTML文件到OSS路径: {}", htmlOssPath);
            
            // 获取下载链接（有效期设置为1小时）
            String htmlDownloadUrl = ossService.getFileUrl(ossService.getDirPrefix() + htmlOssPath, 3600);
            log.info("HTML文件下载链接: {}", htmlDownloadUrl);
            
            // 计算处理时间
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("个人档案配置生成完成，总耗时: {}ms", processingTime);
            
            // 在返回的消息中添加HTML下载链接
            String resultMessage = "个人档案配置文件已生成并保存，可以通过以下链接下载HTML文件";
            return ResponseEntity.ok(
                    ResumeResponse.successWithConfigAndHtml(
                            resultMessage,
                            configFiles,
                            htmlDownloadUrl,
                            processingTime
                    ));
            
        } catch (IllegalArgumentException e) {
            log.error("参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResumeResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("文件处理错误: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResumeResponse.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("未预期的错误: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResumeResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }
    
    /**
     * 保存YAML文件到指定目录
     * @return 保存的文件路径数组
     */
    private String[] saveYamlFilesWithKey(String yamlContent, String key) throws IOException {
        // 获取应用程序的资源目录路径
        Path resourcesPath = Paths.get("src/main/resources/static/profile_website/config", key).toAbsolutePath();
        
        // 创建配置目录（如果不存在）
        if (!Files.exists(resourcesPath)) {
            Files.createDirectories(resourcesPath);
        }
        
        // 解析YAML内容（假设内容包含zh.yaml和en.yaml的内容）
        String[] parts = yamlContent.split("---");
        
        if (parts.length >= 2) {
            // 保存中文YAML
            String zhYaml = parts[0].trim();
            Path zhYamlPath = resourcesPath.resolve("zh.yaml");
            Files.writeString(zhYamlPath, zhYaml);
            log.info("中文YAML文件已保存: {}", zhYamlPath);
            
            // 保存英文YAML
            String enYaml = parts[1].trim();
            Path enYamlPath = resourcesPath.resolve("en.yaml");
            Files.writeString(enYamlPath, enYaml);
            log.info("英文YAML文件已保存: {}", enYamlPath);
            
            return new String[]{
                "/profile_website/config/" + key + "/zh.yaml",
                "/profile_website/config/" + key + "/en.yaml"
            };
        } else {
            log.warn("YAML内容格式不符合预期，无法分离中英文内容");
            // 保存为单个文件
            Path yamlPath = resourcesPath.resolve("config.yaml");
            Files.writeString(yamlPath, yamlContent);
            log.info("配置文件已保存: {}", yamlPath);
            
            return new String[]{
                "/profile_website/config/" + key + "/config.yaml"
            };
        }
    }

    /**
     * 验证key是否符合规则（只允许字母、数字、下划线和连字符）
     */
    private boolean isValidKey(String key) {
        return key != null && !key.isEmpty() && KEY_PATTERN.matcher(key).matches();
    }

    /**
     * 使用Node.js生成独立HTML文件
     * 
     * @param key 用户密钥
     * @return 生成的HTML文件路径
     * @throws IOException 如果执行命令失败
     */
    private String generateStandaloneHtml(String key) throws IOException {
        try {
            // 获取项目根目录
            String projectRoot = new File(".").getCanonicalPath();
            
            // 构建Node.js脚本目录路径
            String scriptDir = projectRoot + "/src/main/resources/static/profile_website";
            
            // 构建输出文件路径
            String outputDir = projectRoot + "/temp/" + key;
            String outputFile = outputDir + "/" + key + ".html";
            
            // 确保输出目录存在
            new File(outputDir).mkdirs();
            
            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "node", 
                "build-standalone.js", 
                "--dir", "config/" + key, 
                "--output", outputFile
            );
            
            // 设置工作目录
            processBuilder.directory(new File(scriptDir));
            
            // 重定向错误流到标准输出
            processBuilder.redirectErrorStream(true);
            
            log.info("执行命令: {}", String.join(" ", processBuilder.command()));
            
            // 执行命令
            Process process = processBuilder.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Node输出: {}", line);
                }
            }
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("生成HTML文件失败，退出码: " + exitCode);
            }
            
            log.info("成功生成HTML文件: {}", outputFile);
            return outputFile;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("生成HTML文件时被中断", e);
        }
    }

    /**
     * 记录上传文件的详细信息，便于调试
     */
    private void logFileInfo(MultipartFile file) {
        if (file == null) {
            log.warn("上传的文件为null");
            return;
        }

        try {
            log.info("上传文件信息 - 名称: {}, 大小: {}字节, 内容类型: {}",
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType());
        } catch (Exception e) {
            log.warn("无法获取上传文件信息", e);
        }
    }
} 