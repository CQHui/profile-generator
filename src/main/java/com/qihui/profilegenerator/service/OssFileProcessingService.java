package com.qihui.profilegenerator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class OssFileProcessingService implements FileProcessingService {

    private final OssService ossService;
    private final Tika tika = new Tika();

    /**
     * 处理上传的文件：上传到OSS并提取文本内容
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    @Override
    public String processFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 检测文件类型
            String detectedType = tika.detect(file.getInputStream());
            log.info("检测到的文件类型: {}, 文件名: {}", detectedType, file.getOriginalFilename());

            // 上传文件到OSS
            String ossObjectKey = ossService.uploadFile(file);
            log.info("文件已上传到OSS，路径: {}", ossObjectKey);

            // 根据文件类型处理
            if (detectedType.contains("text/plain")) {
                // 文本文件直接读取内容
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else if (detectedType.contains("application/pdf")) {
                // PDF文件处理
                return processPdfFile(file, ossObjectKey);
            } else {
                ossService.deleteFile(ossObjectKey); // 删除不支持的文件
                throw new IllegalArgumentException("不支持的文件类型: " + detectedType + "，请上传文本或PDF文件");
            }
        } catch (Exception e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理PDF文件，并指定OSS路径
     * @param file PDF文件
     * @param ossPath 指定的OSS路径
     * @return 提取的文本内容
     */
    @Override
    public String processPdfFile(MultipartFile file, String ossPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF文件不能为空");
        }

        log.info("处理PDF文件: {}, 指定OSS路径: {}", file.getOriginalFilename(), ossPath);
            
            // 上传文件到OSS指定路径
        String ossObjectKey = null;
        try {
            ossObjectKey = ossService.uploadFileToPath(file, ossPath);
        } catch (IOException e) {
            log.info("上传oss失败");
            throw new RuntimeException(e);
        }
        log.info("PDF文件已上传到OSS，路径: {}", ossObjectKey);
            // 首先尝试直接从上传的文件流解析
        try {
            log.info("尝试直接从上传的文件流解析PDF");
            return extractPdfTextWithSpringAI(ossObjectKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    
    /**
     * 使用Spring AI从OSS路径提取PDF文本
     */
    private String extractPdfTextWithSpringAI(String ossObjectKey) throws IOException {
        log.info("使用Spring AI从OSS路径解析PDF: {}", ossObjectKey);
        
        // 获取文件URL
        String fileUrl = ossService.getFileUrl(ossObjectKey, 60);
        if (fileUrl == null) {
            throw new IOException("无法获取PDF文件URL");
        }
        
        // 配置PDF阅读器
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(70)
                .withPageBottomMargin(70)
                .build();
        
        // 使用Spring AI的PDF阅读器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(fileUrl, config);
        
        try {
            List<Document> documents = reader.get();
            
            if (documents.isEmpty()) {
                throw new IOException("PDF文档没有可提取的内容");
            }
            
            // 合并所有页面内容
            String content = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
                
            log.info("Spring AI成功提取PDF文本，页数: {}, 总长度: {} 字符", 
                    documents.size(), content.length());
            
            return content;
        } catch (Exception e) {
            log.error("Spring AI提取PDF文本失败: {}", e.getMessage(), e);
            throw new IOException("无法使用Spring AI提取PDF内容: " + e.getMessage(), e);
        }
    }

    /**
     * 处理文本文件
     * @param file 文本文件
     * @return 文本内容
     */
    public String processTextFile(MultipartFile file, String ossPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文本文件不能为空");
        }
        
        try {
            log.info("处理文本文件: {}, 指定OSS路径: {}", file.getOriginalFilename(), ossPath);
            
            // 上传文件到OSS指定路径
            ossService.uploadFileToPath(file, ossPath);
            
            // 直接读取文本内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("成功读取文本文件内容，长度: {} 字符", content.length());
            
            return content;
        } catch (Exception e) {
            log.error("文本文件处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本文件处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到OSS指定路径
     * @param file 本地文件
     * @param ossPath OSS存储路径
     * @return OSS上的文件路径
     */
    public String uploadFile(File file, String ossPath) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("本地文件不存在");
        }
        
        try {
            log.info("上传本地文件: {}, 指定OSS路径: {}", file.getName(), ossPath);
            
            // 调用OSS服务上传文件
            String objectKey = ossService.uploadFile(file, ossPath);
            log.info("成功上传本地文件到OSS，路径: {}", objectKey);
            
            return objectKey;
        } catch (Exception e) {
            log.error("本地文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("本地文件上传失败: " + e.getMessage(), e);
        }
    }
} 