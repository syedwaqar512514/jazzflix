package org.jazz.jazzflix.config;

import org.jazz.jazzflix.dto.Response;
import org.jazz.jazzflix.exception.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.ConnectIOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.SQLRecoverableException;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final HttpServletRequest request;

    public GlobalExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    // --------------------- Validation ---------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> ResponseEntity<Response<T>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(false, message, null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> ResponseEntity<Response<T>> handleDataAlreadyExistsException(DataAlreadyExistsException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public <T> ResponseEntity<Response<T>> handleDataNotFoundException(DataNotFoundException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(VideoStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> ResponseEntity<Response<T>> handleVideoStorageException(VideoStorageException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> ResponseEntity<Response<T>> handleInvalidRuleException(InvalidRuleException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRuleQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> ResponseEntity<Response<T>> handleInvalidRuleQueryException(InvalidRuleQueryException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRuleQueryReasonException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> ResponseEntity<Response<T>> handleInvalidRuleQueryReasonException(InvalidRuleQueryReasonException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    // --------------------- System / Server Exceptions ---------------------
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> ResponseEntity<Response<T>> handleGeneralException(Exception ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public <T> ResponseEntity<Response<T>> handleNoSuchAlgorithmException(NoSuchAlgorithmException ex) {
        return buildResponse(false, "Algorithm not found: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public <T> ResponseEntity<Response<T>> handleDataAccessException(DataAccessException ex) {
        return buildResponse(false, "Database error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SQLException.class)
    public <T> ResponseEntity<Response<T>> handleSQLException(SQLException ex) {
        return buildResponse(false, "SQL error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SQLDataException.class)
    public <T> ResponseEntity<Response<T>> handleSQLDataException(SQLDataException ex) {
        return buildResponse(false, "SQL data error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SQLRecoverableException.class)
    public <T> ResponseEntity<Response<T>> handleSQLRecoverableException(SQLRecoverableException ex) {
        return buildResponse(false, "SQL recoverable error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SocketTimeoutException.class)
    public <T> ResponseEntity<Response<T>> handleSocketTimeoutException(SocketTimeoutException ex) {
        return buildResponse(false, "Socket timeout: " + ex.getMessage(), null, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(SocketException.class)
    public <T> ResponseEntity<Response<T>> handleSocketException(SocketException ex) {
        return buildResponse(false, "Socket error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConnectIOException.class)
    public <T> ResponseEntity<Response<T>> handleConnectIOException(ConnectIOException ex) {
        return buildResponse(false, "Connection I/O error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IOException.class)
    public <T> ResponseEntity<Response<T>> handleIOException(IOException ex) {
        return buildResponse(false, "I/O error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public <T> ResponseEntity<Response<T>> handleJsonProcessingException(JsonProcessingException ex) {
        return buildResponse(false, "JSON processing error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JsonParseException.class)
    public <T> ResponseEntity<Response<T>> handleJsonParseException(JsonParseException ex) {
        return buildResponse(false, "JSON parse error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ClassNotFoundException.class)
    public <T> ResponseEntity<Response<T>> handleClassNotFoundException(ClassNotFoundException ex) {
        return buildResponse(false, "Class not found: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NumberFormatException.class)
    public <T> ResponseEntity<Response<T>> handleNumberFormatException(NumberFormatException ex) {
        return buildResponse(false, "Number format error: " + ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClassCastException.class)
    public <T> ResponseEntity<Response<T>> handleClassCastException(ClassCastException ex) {
        return buildResponse(false, "Class cast error: " + ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RestClientException.class)
    public <T> ResponseEntity<Response<T>> handleRestClientException(RestClientException ex) {
        return buildResponse(false, "REST client error: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public <T> ResponseEntity<Response<T>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return buildResponse(false, "HTTP method not allowed: " + ex.getMethod(), null, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public <T> ResponseEntity<Response<T>> handleUnauthorized(HttpClientErrorException.Unauthorized ex) {
        return buildResponse(false, "Unauthorized: " + ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CustomException.class)
    public <T> ResponseEntity<Response<T>> handleCustomException(CustomException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.OK);
    }

    @ExceptionHandler(CustomWithCodeException.class)
    public <T> ResponseEntity<Response<T>> handleCustomWithCodeException(CustomWithCodeException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.OK);
    }

    @ExceptionHandler(CustomDataNotFoundException.class)
    public <T> ResponseEntity<Response<T>> handleCustomDataNotFoundException(CustomDataNotFoundException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.OK);
    }

    // --------------------- Utility Method ---------------------
    private <T> ResponseEntity<Response<T>> buildResponse(boolean success, String message, T data, HttpStatus status) {
        Response<T> response = new Response<>();
        response.setSuccess(success);
        response.setMessage(message);
        response.setData(data);
        response.setStatus(status.value());
        response.setTimestamp(Instant.now().toString());
        response.setPath(request.getRequestURI());
        return new ResponseEntity<>(response, status);
    }
}
