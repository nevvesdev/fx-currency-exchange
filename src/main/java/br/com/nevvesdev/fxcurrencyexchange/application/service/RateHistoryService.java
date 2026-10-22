package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.InvalidHistoryRangeException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateHistory;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetRateHistoryUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class RateHistoryService implements GetRateHistoryUseCase {

    static final int DEFAULT_RANGE_DAYS = 30;
    static final long MAX_RANGE_DAYS = 366;

    private final ExchangeRateRepository repository;
    private final Clock clock;

    public RateHistoryService(ExchangeRateRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository é obrigatório");
        this.clock = Objects.requireNonNull(clock, "clock é obrigatório");
    }

    @Override
    public RateHistory getHistory(CurrencyCode base, CurrencyCode quote, LocalDate from, LocalDate to) {
        Objects.requireNonNull(base, "base é obrigatória");
        Objects.requireNonNull(quote, "quote é obrigatória");
        if (base.equals(quote)) {
            throw new InvalidExchangeRateException("Moeda base e moeda cotada devem ser diferentes");
        }

        LocalDate effectiveTo = to != null ? to : LocalDate.now(clock);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new InvalidHistoryRangeException("A data inicial não pode ser posterior à data final");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > MAX_RANGE_DAYS) {
            throw new InvalidHistoryRangeException(
                    "O intervalo máximo do histórico é de " + MAX_RANGE_DAYS + " dias");
        }

        List<ExchangeRate> rates =
                repository.findHistory(base, quote, startOfDay(effectiveFrom), startOfDay(effectiveTo));
        return new RateHistory(base, quote, effectiveFrom, effectiveTo, rates);
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}