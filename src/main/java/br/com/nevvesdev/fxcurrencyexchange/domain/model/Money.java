package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(amount, "amount é obrigatório");
        Objects.requireNonNull(currency, "currency é obrigatório");
        if (amount.signum() < 0) {
            throw new InvalidAmountException("Valor monetário não pode ser negativo");
        }
        amount = amount.setScale(currency.fractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Money of(BigDecimal amount, CurrencyCode currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(parse(amount), CurrencyCode.of(currency));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private static BigDecimal parse(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Valor monetário inválido: " + amount);
        }
    }
}