package com.fleettrack.common.exception;

public class StaleVersionException extends RuntimeException {

    public StaleVersionException(String message) {
        super(message);
    }
}