package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ExchangeRatePersistenceAdapter implements ExchangeRateRepository {

    private final ExchangeRateJpaRepository jpaRepository;
    private final Clock clock;

    public ExchangeRatePersistenceAdapter(ExchangeRateJpaRepository jpaRepository, Clock clock) {
        this.jpaRepository = jpaRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int saveAll(Collection<ExchangeRate> rates) {
        Objects.requireNonNull(rates, "rates é obrigatório");
        Instant fetchedAt = clock.instant();
        int inserted = 0;
        for (ExchangeRate rate : rates) {
            inserted += jpaRepository.insertIfAbsent(
                    rate.base().value(),
                    rate.quote().value(),
                    rate.rate(),
                    rate.quotedAt(),
                    fetchedAt);
        }
        return inserted;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRate> findLatest(CurrencyCode base, CurrencyCode quote) {
        return jpaRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyOrderByQuotedAtDesc(base.value(), quote.value())
                .map(ExchangeRateEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRate> findHistory(CurrencyCode base, CurrencyCode quote, Instant from, Instant to) {
        return jpaRepository
                .findByBaseCurrencyAndQuoteCurrencyAndQuotedAtBetweenOrderByQuotedAtDesc(
                        base.value(), quote.value(), from, to)
                .stream()
                .map(ExchangeRateEntityMapper::toDomain)
                .toList();
    }
}