package com.poc.trademanager.dto;

public class ErrorResponse {
    private String errorCode;
    private String message;
    private String hint;

    public ErrorResponse(String errorCode, String message, String hint) {
        this.errorCode = errorCode;
        this.message = message;
        this.hint = hint;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
