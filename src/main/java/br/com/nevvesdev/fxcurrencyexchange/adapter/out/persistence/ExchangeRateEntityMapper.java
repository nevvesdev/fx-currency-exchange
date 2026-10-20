package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

final class ExchangeRateEntityMapper {

    private ExchangeRateEntityMapper() {
    }

    static ExchangeRate toDomain(ExchangeRateEntity entity) {
        return new ExchangeRate(
                CurrencyCode.of(entity.getBaseCurrency()),
                CurrencyCode.of(entity.getQuoteCurrency()),
                entity.getRate(),
                entity.getQuotedAt());
    }
}