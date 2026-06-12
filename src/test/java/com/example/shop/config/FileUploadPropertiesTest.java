package com.example.shop.config;

import com.example.shop.helper.FileUploadHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件上传助手测试
 */
@SpringBootTest
public class FileUploadPropertiesTest {

    @Autowired
    private FileUploadHelper fileUploadHelper;

    @Test
    public void testConfigurationLoading() {
        // 测试配置是否正确加载
        assertNotNull(fileUploadHelper);
        assertNotNull(fileUploadHelper.getBasePath());

        // 测试默认值
        assertEquals("D:/javawork2/shop/uploads/", fileUploadHelper.getBasePath());
        assertEquals(5242880L, fileUploadHelper.getMaxSize()); // 5MB
        assertEquals("/uploads", fileUploadHelper.getAccessUrlPrefix());
    }

    @Test
    public void testPathGeneration() {
        // 测试路径生成
        assertTrue(fileUploadHelper.getAvatarPath().contains("avatars"));
        assertTrue(fileUploadHelper.getProductPath().contains("products"));
        assertTrue(fileUploadHelper.getReviewPath().contains("reviews"));

        // 测试路径包含基础路径关键部分
        assertTrue(fileUploadHelper.getAvatarPath().contains("uploads"));
        assertTrue(fileUploadHelper.getProductPath().contains("uploads"));
        assertTrue(fileUploadHelper.getReviewPath().contains("uploads"));
    }

    @Test
    public void testAccessUrlGeneration() {
        // 测试访问URL生成
        String avatarUrl = fileUploadHelper.getAvatarAccessUrl("test.jpg");
        assertEquals("/uploads/avatars/test.jpg", avatarUrl);

        String productUrl = fileUploadHelper.getProductAccessUrl("product.png");
        assertEquals("/uploads/products/product.png", productUrl);

        String reviewUrl = fileUploadHelper.getReviewAccessUrl("review.gif");
        assertEquals("/uploads/reviews/review.gif", reviewUrl);
    }

    @Test
    public void testUrlPrefixGeneration() {
        // 测试URL前缀生成
        assertEquals("/uploads/avatars/", fileUploadHelper.getAvatarUrlPrefix());
        assertEquals("/uploads/products/", fileUploadHelper.getProductUrlPrefix());
        assertEquals("/uploads/reviews/", fileUploadHelper.getReviewUrlPrefix());
    }

    @Test
    public void testAllowedExtensions() {
        // 测试允许的扩展名
        assertTrue(fileUploadHelper.isAllowedExtension("jpg"));
        assertTrue(fileUploadHelper.isAllowedExtension("jpeg"));
        assertTrue(fileUploadHelper.isAllowedExtension("png"));
        assertTrue(fileUploadHelper.isAllowedExtension("gif"));
        assertTrue(fileUploadHelper.isAllowedExtension("bmp"));
        assertTrue(fileUploadHelper.isAllowedExtension("webp"));

        // 测试大小写不敏感
        assertTrue(fileUploadHelper.isAllowedExtension("JPG"));
        assertTrue(fileUploadHelper.isAllowedExtension("PNG"));

        // 测试不允许的扩展名
        assertFalse(fileUploadHelper.isAllowedExtension("txt"));
        assertFalse(fileUploadHelper.isAllowedExtension("pdf"));
        assertFalse(fileUploadHelper.isAllowedExtension("doc"));
        assertFalse(fileUploadHelper.isAllowedExtension(null));
    }

    @Test
    public void testUtilityMethods() {
        // 测试文件名提取
        String fileName = fileUploadHelper.extractFileName("/uploads/avatars/test.jpg", "/uploads/avatars/");
        assertEquals("test.jpg", fileName);

        // 测试无效URL
        String invalidFileName = fileUploadHelper.extractFileName("/other/path/test.jpg", "/uploads/avatars/");
        assertNull(invalidFileName);
    }
}
