package com.example.shop.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 静态资源控制器
 */
@RestController
public class StaticResourceController {

    /**
     * 提供API测试页面
     */
    @GetMapping(value = "/api-test.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getApiTestPage() {
        try {
            Resource resource = new ClassPathResource("static/api-test.html");
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
