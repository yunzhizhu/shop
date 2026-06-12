package com.example.shop.helper;

import com.example.shop.exception.BusinessException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件上传助手类
 * 集成配置、工具方法、Web配置于一体
 */
@Component
@ConfigurationProperties(prefix = "file.upload")
@Data
public class FileUploadHelper {
    
    // ========== 配置属性（从yml文件读取） ==========
    private String basePath;
    private Long maxSize;
    private List<String> allowedExtensions;
    private String accessUrlPrefix;
    private String serverBaseUrl;  // 新增：服务器基础URL
    
    // 子目录常量
    private static final String AVATAR_DIR = "avatars";
    private static final String PRODUCT_DIR = "products";
    private static final String REVIEW_DIR = "reviews";
    private static final String MESSAGE_DIR = "messages";
    
    // ========== 路径生成方法 ==========
    public String getAvatarPath() {
        return Paths.get(basePath, AVATAR_DIR).toString() + "/";
    }
    
    public String getProductPath() {
        return Paths.get(basePath, PRODUCT_DIR).toString() + "/";
    }
    
    public String getReviewPath() {
        return Paths.get(basePath, REVIEW_DIR).toString() + "/";
    }
    
    public String getMessagePath() {
        return Paths.get(basePath, MESSAGE_DIR).toString() + "/";
    }
    
    // ========== URL生成方法 ==========
    public String getAvatarAccessUrl(String fileName) {
        return accessUrlPrefix + "/" + AVATAR_DIR + "/" + fileName;
    }
    
    public String getProductAccessUrl(String fileName) {
        return accessUrlPrefix + "/" + PRODUCT_DIR + "/" + fileName;
    }
    
    public String getReviewAccessUrl(String fileName) {
        return accessUrlPrefix + "/" + REVIEW_DIR + "/" + fileName;
    }
    
    public String getMessageAccessUrl(String fileName) {
        return accessUrlPrefix + "/" + MESSAGE_DIR + "/" + fileName;
    }
    
    // ========== 完整URL生成方法（包含服务器地址） ==========
    public String getFullAvatarUrl(String fileName) {
        if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
            return serverBaseUrl + accessUrlPrefix + "/" + AVATAR_DIR + "/" + fileName;
        }
        return getAvatarAccessUrl(fileName);
    }
    
    public String getFullProductUrl(String fileName) {
        if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
            return serverBaseUrl + accessUrlPrefix + "/" + PRODUCT_DIR + "/" + fileName;
        }
        return getProductAccessUrl(fileName);
    }
    
    public String getFullReviewUrl(String fileName) {
        if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
            return serverBaseUrl + accessUrlPrefix + "/" + REVIEW_DIR + "/" + fileName;
        }
        return getReviewAccessUrl(fileName);
    }
    
    public String getFullMessageUrl(String fileName) {
        if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
            return serverBaseUrl + accessUrlPrefix + "/" + MESSAGE_DIR + "/" + fileName;
        }
        return getMessageAccessUrl(fileName);
    }
    
    /**
     * 将相对URL转换为完整URL，同时替换旧的局域网地址
     */
    public String toFullUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return relativeUrl;
        }
        // 如果是旧的局域网完整URL，提取路径部分重新拼接
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
                // 找到 /uploads 路径，替换域名部分
                int pathStart = relativeUrl.indexOf("/uploads");
                if (pathStart != -1) {
                    return serverBaseUrl + relativeUrl.substring(pathStart);
                }
            }
            return relativeUrl;
        }
        // 拼接完整URL
        if (serverBaseUrl != null && !serverBaseUrl.isEmpty()) {
            return serverBaseUrl + relativeUrl;
        }
        return relativeUrl;
    }
    
    // ========== URL前缀方法（用于文件删除时判断） ==========
    public String getAvatarUrlPrefix() {
        return accessUrlPrefix + "/" + AVATAR_DIR + "/";
    }
    
    public String getProductUrlPrefix() {
        return accessUrlPrefix + "/" + PRODUCT_DIR + "/";
    }
    
    public String getReviewUrlPrefix() {
        return accessUrlPrefix + "/" + REVIEW_DIR + "/";
    }
    
    public String getMessageUrlPrefix() {
        return accessUrlPrefix + "/" + MESSAGE_DIR + "/";
    }
    
    // ========== 文件验证方法 ==========
    public boolean isAllowedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return allowedExtensions.contains(extension.toLowerCase());
    }
    
    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw new BusinessException(400, "只支持图片文件");
        }
        
        if (file.getSize() > maxSize) {
            throw new BusinessException(400, "文件大小不能超过" + (maxSize / 1024 / 1024) + "MB");
        }
    }
    
    private boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return isAllowedExtension(extension);
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    // ========== 工具方法 ==========
    public String extractFileName(String fileUrl, String urlPrefix) {
        if (fileUrl != null && fileUrl.startsWith(urlPrefix)) {
            return fileUrl.substring(urlPrefix.length());
        }
        return null;
    }
    
    public Path buildFilePath(String directory, String fileName) {
        return Paths.get(directory, fileName);
    }
    
    public void createDirectoryIfNotExists(String path) throws IOException {
        Path uploadPath = Paths.get(path);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }
    
    // ========== Web配置方法 ==========
    public void configureResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保路径以 / 结尾
        String locationPath = basePath.endsWith("/") ? basePath : basePath + "/";
        
        registry.addResourceHandler(accessUrlPrefix + "/**")
                .addResourceLocations("file:" + locationPath);
    }
}
