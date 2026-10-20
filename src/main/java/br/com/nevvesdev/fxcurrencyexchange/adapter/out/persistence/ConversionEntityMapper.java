package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Money;

final class ConversionEntityMapper {

    private ConversionEntityMapper() {
    }

    static ConversionEntity toEntity(Conversion conversion) {
        return new ConversionEntity(
                conversion.id(),
                conversion.source().currency().value(),
                conversion.source().amount(),
                conversion.result().currency().value(),
                conversion.result().amount(),
                conversion.rate().rate(),
                conversion.rate().quotedAt(),
                conversion.convertedAt());
    }

    static Conversion toDomain(ConversionEntity entity) {
        CurrencyCode source = CurrencyCode.of(entity.getSourceCurrency());
        CurrencyCode result = CurrencyCode.of(entity.getResultCurrency());
        ExchangeRate rate = new ExchangeRate(source, result, entity.getRate(), entity.getRateQuotedAt());
        return new Conversion(
                entity.getId(),
                new Money(entity.getSourceAmount(), source),
                new Money(entity.getResultAmount(), result),
                rate,
                entity.getConvertedAt());
    }
}