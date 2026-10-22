package br.com.nevvesdev.fxcurrencyexchange.application.exception;

import java.util.UUID;

public final class ConversionNotFoundException extends RuntimeException {

    public ConversionNotFoundException(UUID id) {
        super("Conversão não encontrada: " + id);
    }
}