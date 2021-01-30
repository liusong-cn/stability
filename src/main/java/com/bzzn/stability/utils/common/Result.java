package com.bzzn.stability.utils.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: ls
 * @date: 2021/1/30 14:03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    String msg;

    int code;

    T data;

    public  static <T>  Result<T> success(T data){
        Result result = new Result(200, "success");
        result.setData(data);
        return result;
    }

    public Result(int code, String msg){
        this.code = code;
        this.msg = msg;
    }


}
