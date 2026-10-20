package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversions")
public class ConversionEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "source_amount", nullable = false, precision = 28, scale = 4)
    private BigDecimal sourceAmount;

    @Column(name = "result_currency", nullable = false, length = 3)
    private String resultCurrency;

    @Column(name = "result_amount", nullable = false, precision = 28, scale = 4)
    private BigDecimal resultAmount;

    @Column(name = "rate", nullable = false, precision = 40, scale = 20)
    private BigDecimal rate;

    @Column(name = "rate_quoted_at", nullable = false)
    private Instant rateQuotedAt;

    @Column(name = "converted_at", nullable = false)
    private Instant convertedAt;

    @Transient
    private boolean newEntity = true;

    protected ConversionEntity() {
    }

    public ConversionEntity(
            UUID id,
            String sourceCurrency,
            BigDecimal sourceAmount,
            String resultCurrency,
            BigDecimal resultAmount,
            BigDecimal rate,
            Instant rateQuotedAt,
            Instant convertedAt) {
        this.id = id;
        this.sourceCurrency = sourceCurrency;
        this.sourceAmount = sourceAmount;
        this.resultCurrency = resultCurrency;
        this.resultAmount = resultAmount;
        this.rate = rate;
        this.rateQuotedAt = rateQuotedAt;
        this.convertedAt = convertedAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.newEntity = false;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public String getResultCurrency() {
        return resultCurrency;
    }

    public BigDecimal getResultAmount() {
        return resultAmount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getRateQuotedAt() {
        return rateQuotedAt;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }
}