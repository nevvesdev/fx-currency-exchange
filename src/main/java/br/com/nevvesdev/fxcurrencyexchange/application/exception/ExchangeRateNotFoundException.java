package br.com.nevvesdev.fxcurrencyexchange.application.exception;

public final class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }
}