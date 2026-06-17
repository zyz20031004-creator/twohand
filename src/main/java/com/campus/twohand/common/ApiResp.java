package com.campus.twohand.common;

/**
 * 统一返回结构
 * code: 0=成功, 1=失败, 401=需要重新登录（密码修改后触发）
 * msg : 提示信息
 * data: 业务数据
 */
public class ApiResp<T> {

    private int code;
    private String msg;
    private T data;

    public static final int CODE_OK = 0;
    public static final int CODE_FAIL = 1;
    public static final int CODE_RELOGIN = 401; // 需要重新登录（密码修改后使用）

    public static <T> ApiResp<T> ok(T data) {
        ApiResp<T> r = new ApiResp<>();
        r.code = CODE_OK;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResp<T> fail(String msg) {
        ApiResp<T> r = new ApiResp<>();
        r.code = CODE_FAIL;
        r.msg = msg;
        r.data = null;
        return r;
    }

    /**
     * 创建需要重新登录的响应
     * 用于密码修改成功后，通知前端清除登录状态并跳转到登录页
     *
     * @param message 提示信息
     * @param <T>    泛型类型
     * @return ApiResp 实例，code=401
     */
    public static <T> ApiResp<T> reLogin(String message) {
        ApiResp<T> r = new ApiResp<>();
        r.code = CODE_RELOGIN;
        r.msg = message;
        r.data = null;
        return r;
    }

    /**
     * 判断响应是否表示需要重新登录
     */
    public boolean isRelogin() {
        return code == CODE_RELOGIN;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
