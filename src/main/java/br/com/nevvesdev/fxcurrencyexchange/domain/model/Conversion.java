package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.CurrencyMismatchException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Conversion(UUID id, Money source, Money result, ExchangeRate rate, Instant convertedAt) {

    public Conversion {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(source, "source é obrigatório");
        Objects.requireNonNull(result, "result é obrigatório");
        Objects.requireNonNull(rate, "rate é obrigatória");
        Objects.requireNonNull(convertedAt, "convertedAt é obrigatório");
        if (!source.isPositive()) {
            throw new InvalidAmountException("Valor a converter deve ser maior que zero");
        }
        if (!source.currency().equals(rate.base()) || !result.currency().equals(rate.quote())) {
            throw new CurrencyMismatchException("Moedas da conversão não correspondem à taxa aplicada");
        }
    }

    public static Conversion perform(Money source, ExchangeRate rate, Instant convertedAt) {
        Objects.requireNonNull(source, "source é obrigatório");
        Objects.requireNonNull(rate, "rate é obrigatória");
        return new Conversion(UUID.randomUUID(), source, rate.convert(source), rate, convertedAt);
    }
}