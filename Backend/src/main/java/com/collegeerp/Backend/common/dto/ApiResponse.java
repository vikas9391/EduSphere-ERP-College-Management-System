package com.collegeerp.Backend.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Uniform response envelope returned by every controller in the application.
 * <p>
 * Success example:
 * {@code {"success":true,"message":"OK","data":{...},"timestamp":"..."}}
 * <p>
 * Error example (built by GlobalExceptionHandler):
 * {@code {"success":false,"message":"Student not found","status":404,"path":"/api/students/5","timestamp":"..."}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Integer status;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;

    private ApiResponse(boolean success, String message, T data, Integer status, String path) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data, null, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static ApiResponse<Void> error(String message, int status, String path) {
        return new ApiResponse<>(false, message, null, status, path);
    }

    public static <T> ApiResponse<T> error(String message, T data, int status, String path) {
        return new ApiResponse<>(false, message, data, status, path);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Integer getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
