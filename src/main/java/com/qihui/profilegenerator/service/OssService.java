package com.qihui.profilegenerator.service;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class OssService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Getter
    @Value("${aliyun.oss.dir-prefix}")
    private String dirPrefix;

    @Getter
    @Value("${aliyun.oss.url-prefix}")
    private String urlPrefix;

    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @return OSS上的文件路径
     * @throws IOException 如果文件处理失败
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 生成唯一文件名，避免文件覆盖
        String originalFilename = file.getOriginalFilename();
        String objectName = dirPrefix + generateUniqueFileName(getFileSuffix(originalFilename));
        
        return uploadFileToPath(file, objectName);
    }

    /**
     * 上传文件到OSS指定路径
     * @param file 要上传的文件
     * @param customPath 自定义的OSS路径（不包含dirPrefix）
     * @return OSS上的文件路径
     * @throws IOException 如果文件处理失败
     */
    public String uploadFileToPath(MultipartFile file, String customPath) throws IOException {
        String objectName = dirPrefix + customPath;
        log.info("上传文件到指定路径: {}", objectName);
        
        OSS ossClient = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 上传文件流
            ossClient.putObject(bucketName, objectName, file.getInputStream());
            
            log.info("上传文件到OSS成功: {}", objectName);
            return objectName;
        } catch (OSSException oe) {
            log.error("OSS服务端异常: {}", oe.getMessage(), oe);
            throw new IOException("上传到阿里云OSS失败: " + oe.getMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage(), ce);
            throw new IOException("无法连接到阿里云OSS: " + ce.getMessage(), ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 获取OSS上的文件内容
     * @param objectName OSS上的文件路径
     * @return 文件内容
     * @throws IOException 如果文件处理失败
     */
    public String getFileContent(String objectName) throws IOException {
        OSS ossClient = null;
        OSSObject ossObject = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 获取OSS对象
            ossObject = ossClient.getObject(bucketName, objectName);
            
            // 读取文件内容
            try (InputStream inputStream = ossObject.getObjectContent()) {
                byte[] content = inputStream.readAllBytes();
                return new String(content);
            }
        } catch (OSSException | ClientException e) {
            log.error("获取OSS文件失败: {}", e.getMessage(), e);
            throw new IOException("获取阿里云OSS文件失败: " + e.getMessage(), e);
        } finally {
            // 关闭OSSObject
            if (ossObject != null) {
                try {
                    ossObject.close();
                } catch (IOException e) {
                    log.warn("关闭OSS对象失败", e);
                }
            }
            
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 获取OSS文件的二进制内容
     * @param objectName OSS上的文件路径
     * @return 文件的二进制内容
     * @throws IOException 如果文件处理失败
     */
    public byte[] getFileBytes(String objectName) throws IOException {
        OSS ossClient = null;
        OSSObject ossObject = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 获取OSS对象
            ossObject = ossClient.getObject(bucketName, objectName);
            
            // 读取文件二进制内容
            try (InputStream inputStream = ossObject.getObjectContent();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                return outputStream.toByteArray();
            }
        } catch (OSSException e) {
            log.error("OSS服务端异常: {}", e.getMessage(), e);
            throw new IOException("获取阿里云OSS文件失败: " + e.getMessage(), e);
        } catch (ClientException e) {
            log.error("OSS客户端异常: {}", e.getMessage(), e);
            throw new IOException("无法连接到阿里云OSS: " + e.getMessage(), e);
        } finally {
            // 关闭OSSObject
            if (ossObject != null) {
                try {
                    ossObject.close();
                } catch (IOException e) {
                    log.warn("关闭OSS对象失败", e);
                }
            }
            
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 获取文件的临时访问URL
     * @param objectName OSS上的文件路径
     * @param expireInSeconds URL的有效期（秒）
     * @return 访问URL
     */
    public String getFileUrl(String objectName, int expireInSeconds) {
        OSS ossClient = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 设置URL过期时间
            Date expiration = new Date(System.currentTimeMillis() + expireInSeconds * 1000L);
            
            // 生成临时URL
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("生成文件访问URL失败: {}", e.getMessage(), e);
            return null;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 删除OSS上的文件
     * @param objectName OSS上的文件路径
     */
    public void deleteFile(String objectName) {
        OSS ossClient = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 删除文件
            ossClient.deleteObject(bucketName, objectName);
            
            log.info("成功删除文件: {}", objectName);
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 上传文本内容到OSS
     * @param content 文本内容
     * @param suffix 文件后缀
     * @return OSS上的文件路径
     */
    public String uploadText(String content, String suffix) {
        String fileName = generateUniqueFileName(suffix);
        String objectName = dirPrefix + fileName;
        
        OSS ossClient = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 上传文本
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));
            
            log.info("上传文本到OSS成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("上传文本到OSS失败: {}", e.getMessage(), e);
            return null;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 上传本地文件到OSS指定路径
     * @param file 本地文件
     * @param customPath 自定义的OSS路径（不包含dirPrefix）
     * @return OSS上的文件路径
     * @throws IOException 如果文件处理失败
     */
    public String uploadFile(File file, String customPath) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        
        String objectName = dirPrefix + customPath;
        log.info("上传本地文件到指定路径: {}", objectName);
        
        OSS ossClient = null;
        
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 上传文件
            ossClient.putObject(bucketName, objectName, file);
            
            log.info("上传本地文件到OSS成功: {}", objectName);
            return objectName;
        } catch (OSSException oe) {
            log.error("OSS服务端异常: {}", oe.getMessage(), oe);
            throw new IOException("上传到阿里云OSS失败: " + oe.getMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage(), ce);
            throw new IOException("无法连接到阿里云OSS: " + ce.getMessage(), ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 从文件名获取文件后缀
     * @param filename 文件名
     * @return 文件后缀，包括点号(.)
     */
    private String getFileSuffix(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
    
    /**
     * 生成唯一的文件名
     * @param suffix 文件后缀，包括.
     * @return 生成的文件名
     */
    private String generateUniqueFileName(String suffix) {
        // 使用UUID和时间戳组合生成唯一文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + uuid + suffix;
    }
} 