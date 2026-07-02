package cn.chengshuai.csaiagent.common;

/**
 * 业务接口统一返回码。
 */
public enum ResultCode {

    SUCCESS(0, "ok"),
    PARAM_INVALID(40001, "参数校验失败"),
    NOT_FOUND(40400, "资源不存在"),
    INTERNAL_ERROR(50000, "服务内部错误"),
    DEPENDENCY_UNAVAILABLE(50001, "依赖能力不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
