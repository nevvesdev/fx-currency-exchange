package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateEntity, Long> {

    Optional<ExchangeRateEntity> findFirstByBaseCurrencyAndQuoteCurrencyOrderByQuotedAtDesc(
            String baseCurrency, String quoteCurrency);

    List<ExchangeRateEntity> findByBaseCurrencyAndQuoteCurrencyAndQuotedAtBetweenOrderByQuotedAtDesc(
            String baseCurrency, String quoteCurrency, Instant from, Instant to);

    @Modifying
    @Query(value = """
            INSERT INTO exchange_rates (base_currency, quote_currency, rate, quoted_at, fetched_at)
            VALUES (:baseCurrency, :quoteCurrency, :rate, :quotedAt, :fetchedAt)
            ON CONFLICT (base_currency, quote_currency, quoted_at) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("baseCurrency") String baseCurrency,
            @Param("quoteCurrency") String quoteCurrency,
            @Param("rate") BigDecimal rate,
            @Param("quotedAt") Instant quotedAt,
            @Param("fetchedAt") Instant fetchedAt);
}