package com.campus.twohand.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "操作失败，请稍后重试";

    @ExceptionHandler(RuntimeException.class)
    public ApiResp<Void> handleRuntime(RuntimeException e) {
        if (isTechnicalException(e)) {
            log.error("Technical runtime exception", e);
            return ApiResp.fail(GENERIC_ERROR_MESSAGE);
        }
        String message = e.getMessage();
        return ApiResp.fail(message == null || message.isBlank() ? GENERIC_ERROR_MESSAGE : message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResp<Void> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResp.fail(GENERIC_ERROR_MESSAGE);
    }

    private boolean isTechnicalException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataAccessException || current instanceof SQLException) {
                return true;
            }
            String className = current.getClass().getName().toLowerCase();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (className.contains("jdbc")
                    || className.contains("sql")
                    || message.contains("jdbc")
                    || message.contains("sql")
                    || message.contains("constraint")
                    || message.contains("could not execute statement")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
