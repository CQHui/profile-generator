package com.qihui.profilegenerator.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件处理服务接口，定义了处理上传文件的功能
 */
public interface FileProcessingService {

    /**
     * 处理上传的文件并提取文本内容
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    String processFile(MultipartFile file);
    
    /**
     * 处理PDF文件并提取文本内容，指定保存路径
     * @param file PDF文件
     * @param ossPath OSS存储路径
     * @return 提取的文本内容
     */
    String processPdfFile(MultipartFile file, String ossPath);
    
    /**
     * 处理文本文件并提取内容，指定保存路径
     * @param file 文本文件
     * @param ossPath OSS存储路径
     * @return 提取的文本内容
     */
    String processTextFile(MultipartFile file, String ossPath);
} 