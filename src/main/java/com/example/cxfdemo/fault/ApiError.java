package com.example.cxfdemo.fault;

import java.time.OffsetDateTime;

public class ApiError {
    private final boolean success = false;
    private final String code;
    private final String message;
    private final String traceId;
    private final String timestamp;

    public ApiError(String code, String message, String traceId) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = OffsetDateTime.now().toString();
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getTraceId() { return traceId; }
    public String getTimestamp() { return timestamp; }
}
