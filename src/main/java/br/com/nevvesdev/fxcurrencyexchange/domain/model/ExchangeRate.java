package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.CurrencyMismatchException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public record ExchangeRate(CurrencyCode base, CurrencyCode quote, BigDecimal rate, Instant quotedAt) {

    private static final MathContext INVERSION_CONTEXT = new MathContext(18, RoundingMode.HALF_EVEN);

    public ExchangeRate {
        Objects.requireNonNull(base, "base é obrigatória");
        Objects.requireNonNull(quote, "quote é obrigatória");
        Objects.requireNonNull(rate, "rate é obrigatória");
        Objects.requireNonNull(quotedAt, "quotedAt é obrigatório");
        if (base.equals(quote)) {
            throw new InvalidExchangeRateException("Moeda base e moeda cotada devem ser diferentes");
        }
        if (rate.signum() <= 0) {
            throw new InvalidExchangeRateException("Taxa de câmbio deve ser maior que zero");
        }
        rate = normalize(rate);
    }

    public Money convert(Money source) {
        Objects.requireNonNull(source, "source é obrigatório");
        if (!source.currency().equals(base)) {
            throw new CurrencyMismatchException(
                    "Taxa " + base + "/" + quote + " não se aplica a valor em " + source.currency());
        }
        return Money.of(source.amount().multiply(rate), quote);
    }

    public ExchangeRate inverse() {
        return new ExchangeRate(quote, base, BigDecimal.ONE.divide(rate, INVERSION_CONTEXT), quotedAt);
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}