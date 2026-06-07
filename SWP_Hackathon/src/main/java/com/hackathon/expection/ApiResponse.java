package com.hackathon.expection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {
    private boolean status;
    private String message;
    private T data;
    private Object errors;

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> airResponse = new ApiResponse<T>();
        airResponse.status=true;
        airResponse.data=data;
        airResponse.message= message;
        return airResponse;
    }
    public static <T> ApiResponse error(Object errors, String message){
        ApiResponse<T> airResponse = new ApiResponse<T>();
        airResponse.status=false;
        airResponse.errors=errors;
        airResponse.message= message;
        return airResponse;
    }


}
