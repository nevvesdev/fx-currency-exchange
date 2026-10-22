package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.model.ConversionResult;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConvertCurrencyCommand;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.ConvertCurrencyUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class ConvertCurrencyService implements ConvertCurrencyUseCase {

    static final int MAX_INTEGER_DIGITS = 12;

    private final GetExchangeRateUseCase exchangeRates;
    private final ConversionRepository conversions;
    private final Clock clock;

    public ConvertCurrencyService(
            GetExchangeRateUseCase exchangeRates, ConversionRepository conversions, Clock clock) {
        this.exchangeRates = Objects.requireNonNull(exchangeRates, "exchangeRates é obrigatório");
        this.conversions = Objects.requireNonNull(conversions, "conversions é obrigatório");
        this.clock = Objects.requireNonNull(clock, "clock é obrigatório");
    }

    @Override
    public ConversionResult convert(ConvertCurrencyCommand command) {
        Objects.requireNonNull(command, "command é obrigatório");
        Money source = toMoney(command.amount(), command.from());

        ExchangeRateLookup lookup = exchangeRates.getRate(command.from(), command.to());
        Instant convertedAt = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Conversion conversion = Conversion.perform(source, lookup.rate(), convertedAt);

        conversions.save(conversion);
        return new ConversionResult(conversion, lookup.source());
    }

    private static Money toMoney(BigDecimal amount, CurrencyCode currency) {
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Valor a converter deve ser maior que zero");
        }
        if (amount.precision() - amount.scale() > MAX_INTEGER_DIGITS) {
            throw new InvalidAmountException(
                    "Valor excede o máximo de " + MAX_INTEGER_DIGITS + " dígitos inteiros");
        }
        int allowedDigits = currency.fractionDigits();
        if (amount.stripTrailingZeros().scale() > allowedDigits) {
            throw new InvalidAmountException(
                    "O valor possui mais casas decimais do que a moeda " + currency + " permite ("
                            + allowedDigits + ")");
        }
        return Money.of(amount, currency);
    }
}