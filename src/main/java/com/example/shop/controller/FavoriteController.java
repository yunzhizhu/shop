package com.example.shop.controller;

import com.example.shop.common.Result;
import com.example.shop.dto.BatchFavoriteRequest;
import com.example.shop.dto.FavoriteRequest;
import com.example.shop.dto.FavoriteResponse;
import com.example.shop.service.FavoriteService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<String> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            boolean success = favoriteService.addFavorite(userId, request.getProductId());
            if (success) {
                return Result.success("收藏成功");
            } else {
                return Result.error("收藏失败，商品可能不存在或已收藏");
            }
        } catch (Exception e) {
            log.error("添加收藏异常", e);
            return Result.error("系统异常");
        }
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/remove")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<String> removeFavorite(@Valid @RequestBody FavoriteRequest request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            boolean success = favoriteService.removeFavorite(userId, request.getProductId());
            if (success) {
                return Result.success("取消收藏成功");
            } else {
                return Result.error("取消收藏失败");
            }
        } catch (Exception e) {
            log.error("取消收藏异常", e);
            return Result.error("系统异常");
        }
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<Map<String, Object>> checkFavorite(@PathVariable Long productId) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            boolean isFavorite = favoriteService.isFavorite(userId, productId);
            Map<String, Object> result = new HashMap<>();
            result.put("isFavorite", isFavorite);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查收藏状态异常", e);
            return Result.error("系统异常");
        }
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<FavoriteResponse>> getFavoriteList() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            List<FavoriteResponse> favoriteList = favoriteService.getFavoriteList(userId);
            return Result.success(favoriteList);
        } catch (Exception e) {
            log.error("获取收藏列表异常", e);
            return Result.error("系统异常");
        }
    }

    /**
     * 获取收藏数量
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<Map<String, Object>> getFavoriteCount() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            long count = favoriteService.getFavoriteCount(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取收藏数量异常", e);
            return Result.error("系统异常");
        }
    }

    /**
     * 批量删除收藏
     */
    @DeleteMapping("/batch-remove")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<Map<String, Object>> batchRemoveFavorites(@Valid @RequestBody BatchFavoriteRequest request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            int deletedCount = favoriteService.batchRemoveFavorites(userId, request.getProductIds());
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("totalRequested", request.getProductIds().size());
            
            if (deletedCount > 0) {
                return Result.success("批量删除收藏成功", result);
            } else {
                return Result.error("批量删除收藏失败，可能商品未收藏");
            }
        } catch (Exception e) {
            log.error("批量删除收藏异常", e);
            return Result.error("系统异常");
        }
    }
}