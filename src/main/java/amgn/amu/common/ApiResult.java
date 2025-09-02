package amgn.amu.common;

public record ApiResult<T>(
        boolean ok
        , T data
        , String message
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<T>(true, data, null);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<T>(false, null, message);
    }
}
