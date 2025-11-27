package com.faculty.event.event_portal.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 1. Đánh dấu đây là nơi xử lý lỗi cho TOÀN BỘ hệ thống
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi Logic (Ví dụ: Trùng Email, Trùng MSSV, Mật khẩu sai...)
     * Trả về mã 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage()); // Lấy nội dung bạn đã throw

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi Không tìm thấy dữ liệu (Ví dụ: Sai ID sự kiện, User không tồn tại)
     * Trả về mã 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Xử lý lỗi Không có quyền truy cập (Ví dụ: Student cố xóa sự kiện)
     * Trả về mã 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Bạn không có quyền thực hiện hành động này.");

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Xử lý tất cả các lỗi còn lại (Lỗi hệ thống, NullPointer...)
     * Trả về mã 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        e.printStackTrace(); // In lỗi ra console server để debug

        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Xử lý lỗi vi phạm ràng buộc CSDL (Ví dụ: Trùng tên danh mục, trùng email mà code logic chưa bắt được)
     * Trả về mã 400 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");

        // Phân tích thông báo lỗi để trả về câu dễ hiểu hơn
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("ten_danh_muc")) {
                response.put("message", "Tên danh mục này đã tồn tại!");
            } else if (msg.contains("email")) {
                response.put("message", "Email này đã tồn tại!");
            } else if (msg.contains("mssv")) {
                response.put("message", "Mã số sinh viên này đã tồn tại!");
            } else {
                response.put("message", "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc.");
            }
        } else {
            response.put("message", "Lỗi dữ liệu không hợp lệ.");
        }

        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLockedException(LockedException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
}