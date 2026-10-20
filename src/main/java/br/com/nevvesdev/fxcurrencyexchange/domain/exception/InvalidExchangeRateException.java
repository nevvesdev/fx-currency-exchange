package br.com.nevvesdev.fxcurrencyexchange.domain.exception;

public final class InvalidExchangeRateException extends DomainException {

    public InvalidExchangeRateException(String message) {
        super(message);
    }
}