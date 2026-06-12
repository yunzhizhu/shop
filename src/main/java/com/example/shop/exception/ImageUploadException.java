package com.example.shop.exception;

/**
 * 图片上传异常
 */
public class ImageUploadException extends BusinessException {

    public ImageUploadException() {
        super(3006, "图片上传失败");
    }

    public ImageUploadException(String message) {
        super(3006, message);
    }

    public ImageUploadException(String imageUrl, String reason) {
        super(3006, String.format("图片上传失败: imageUrl=%s, reason=%s", imageUrl, reason));
    }
}