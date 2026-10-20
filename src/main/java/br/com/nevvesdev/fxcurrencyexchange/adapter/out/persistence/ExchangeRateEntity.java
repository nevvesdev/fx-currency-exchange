package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(name = "rate", nullable = false, precision = 40, scale = 20)
    private BigDecimal rate;

    @Column(name = "quoted_at", nullable = false)
    private Instant quotedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ExchangeRateEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getQuotedAt() {
        return quotedAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}