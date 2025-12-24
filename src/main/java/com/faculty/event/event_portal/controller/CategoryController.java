package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.CategoryResponse;
import com.faculty.event.event_portal.entity.Category;
import com.faculty.event.event_portal.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final AdminService adminService;

    public CategoryController(AdminService adminService) {
        this.adminService = adminService;
    }

    // API Trang chủ: GET /api/categories
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getPublicCategories() {
        return ResponseEntity.ok(adminService.getAllCategories());
    }
}