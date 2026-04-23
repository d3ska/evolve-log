package com.deska.evolvelog.dto;

public record ApiResponse<T>(
        boolean success,
        T data,
        String error,
        PageMeta meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> paged(T data, long total, int page, int size) {
        return new ApiResponse<>(true, data, null, new PageMeta(total, page, size));
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public record PageMeta(long total, int page, int size) {}
}
