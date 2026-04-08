package com.knowledgebase.common;

/**
 * 文档解析异常
 * 用于区分不同类型的解析错误
 */
public class DocumentParseException extends RuntimeException {

    public enum ErrorType {
        FILE_NOT_FOUND("文件不存在"),
        FILE_READ_ERROR("文件读取失败"),
        INVALID_FORMAT("文件格式无效"),
        ENCRYPTED_FILE("文件已加密，无法解析"),
        EMPTY_CONTENT("文件内容为空"),
        PARSE_ERROR("解析过程出错");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorType errorType;

    public DocumentParseException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public DocumentParseException(ErrorType errorType, String detail) {
        super(errorType.getMessage() + ": " + detail);
        this.errorType = errorType;
    }

    public DocumentParseException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public DocumentParseException(ErrorType errorType, String detail, Throwable cause) {
        super(errorType.getMessage() + ": " + detail, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
