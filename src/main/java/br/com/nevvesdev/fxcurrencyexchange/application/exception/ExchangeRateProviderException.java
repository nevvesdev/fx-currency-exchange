package br.com.nevvesdev.fxcurrencyexchange.application.exception;

public final class ExchangeRateProviderException extends RuntimeException {

    public ExchangeRateProviderException(String message) {
        super(message);
    }

    public ExchangeRateProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}