package com.example.shop.config;

import com.example.shop.helper.FileUploadHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件上传配置类
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Autowired
    private FileUploadHelper fileUploadHelper;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用FileUploadHelper配置静态资源访问
        fileUploadHelper.configureResourceHandlers(registry);
    }
}
