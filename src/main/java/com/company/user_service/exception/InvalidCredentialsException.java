package com.company.user_service.exception;

public class InvalidCredentialsException extends RuntimeException {
     public InvalidCredentialsException(String msg) {
        super(msg);
    }
}
