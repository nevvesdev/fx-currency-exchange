package br.com.nevvesdev.fxcurrencyexchange.domain.exception;

public final class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(String message) {
        super(message);
    }
}