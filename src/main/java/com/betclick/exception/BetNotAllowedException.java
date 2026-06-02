package com.betclick.exception;

public class BetNotAllowedException extends RuntimeException {
    public BetNotAllowedException(String message) {
        super(message);
    }
}
