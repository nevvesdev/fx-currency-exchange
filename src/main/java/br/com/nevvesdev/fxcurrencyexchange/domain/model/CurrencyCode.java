package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidCurrencyException;

import java.util.Currency;
import java.util.Locale;

public record CurrencyCode(String value) {

    public CurrencyCode {
        if (value == null || value.isBlank()) {
            throw new InvalidCurrencyException("Código de moeda é obrigatório");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        Currency currency = resolve(normalized);
        if (currency.getDefaultFractionDigits() < 0) {
            throw new InvalidCurrencyException("Moeda não suportada para câmbio: " + normalized);
        }
        value = normalized;
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    public int fractionDigits() {
        return Currency.getInstance(value).getDefaultFractionDigits();
    }

    @Override
    public String toString() {
        return value;
    }

    private static Currency resolve(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException("Código de moeda inválido: " + code);
        }
    }
}