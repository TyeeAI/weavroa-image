package com.eniac.weavroa.image.entity;

import lombok.Data;

@Data
public class Result {

    private Object data;

    private String message;

    private String code;

    public static Result success(Object data) {
        Result result = new Result();
        result.setData(data);
        result.setMessage("success");
        result.setCode("200");
        return result;
    }
}
