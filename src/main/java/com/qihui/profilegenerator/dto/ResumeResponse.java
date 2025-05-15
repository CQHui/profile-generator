package com.qihui.profilegenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历处理响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误消息（如果有）
     */
    private String error;
    
    /**
     * 处理后的简历文本内容
     */
    private String content;
    
    /**
     * 处理时间（毫秒）
     */
    private long processingTimeMs;
    
    /**
     * 生成的配置文件路径（如果是生成配置）
     */
    private String[] configFiles;
    
    /**
     * HTML文件下载URL（如果有）
     */
    private String htmlUrl;
    
    /**
     * 创建成功响应（用于配置和HTML生成）
     */
    public static ResumeResponse successWithConfigAndHtml(String content, String[] configFiles, String htmlUrl, long processingTimeMs) {
        return ResumeResponse.builder()
                .success(true)
                .content(content)
                .configFiles(configFiles)
                .htmlUrl(htmlUrl)
                .processingTimeMs(processingTimeMs)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static ResumeResponse error(String error) {
        return ResumeResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
} 