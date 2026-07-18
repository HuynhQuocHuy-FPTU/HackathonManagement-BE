package com.hackathon.exception;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ApiResponse<T> {
    private boolean status;
    private String message;
    private T data;
    private Object errors;
    private boolean success;

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> airResponse = new ApiResponse<T>();
        airResponse.status = true;
        airResponse.data = data;
        airResponse.message = message;
        return airResponse;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ok(message, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder().success(false).message(message).data(null).build();
    }

}
