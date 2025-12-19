package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;

public record RsData<T> (
        @NonNull String resultCode,
//        @JsonIgnore int statusCode, //TODO: 추후에 수정
        @NonNull String msg,
        @NonNull T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public static <T> RsData<T> success(String msg, T data) {
        return new RsData<>("200-OK", msg, data);
    }
}
