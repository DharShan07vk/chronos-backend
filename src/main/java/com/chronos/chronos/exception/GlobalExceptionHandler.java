package com.chronos.chronos.exception;

import com.chronos.chronos.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiResponse<>(false, ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatusCode.valueOf(413))
                .body(new ApiResponse<>(
                        false,
                        "File size exceeds the 50MB limit. Please choose a smaller file.",
                        "FILE_SIZE_LIMIT_EXCEEDED"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMultipartException(MultipartException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof MaxUploadSizeExceededException) {
            return ResponseEntity.status(HttpStatusCode.valueOf(413))
                    .body(new ApiResponse<>(
                            false,
                            "File size exceeds the 50MB limit. Please choose a smaller file.",
                            "FILE_SIZE_LIMIT_EXCEEDED"));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        false,
                        "Invalid multipart request.",
                        "MULTIPART_REQUEST_INVALID"));
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MissingServletRequestParameterException.class })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        false,
                        "Invalid request payload.",
                        "INVALID_REQUEST_PAYLOAD"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        false,
                        "Something went wrong. Please try again.",
                        "INTERNAL_SERVER_ERROR"));
    }
}