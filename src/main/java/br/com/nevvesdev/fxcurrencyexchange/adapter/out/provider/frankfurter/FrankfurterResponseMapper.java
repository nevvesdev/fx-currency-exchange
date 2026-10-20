package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.DomainException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

final class FrankfurterResponseMapper {

    private FrankfurterResponseMapper() {
    }

    static List<ExchangeRate> toExchangeRates(FrankfurterResponse response) {
        validate(response);
        try {
            CurrencyCode base = CurrencyCode.of(response.base());
            Instant quotedAt = response.date().atStartOfDay(ZoneOffset.UTC).toInstant();
            return response.rates().entrySet().stream()
                    .map(entry -> new ExchangeRate(base, CurrencyCode.of(entry.getKey()), entry.getValue(), quotedAt))
                    .toList();
        } catch (DomainException e) {
            throw new ExchangeRateProviderException("Resposta do provedor de câmbio contém dados inválidos", e);
        }
    }

    private static void validate(FrankfurterResponse response) {
        if (response == null
                || response.base() == null
                || response.date() == null
                || response.rates() == null
                || response.rates().values().stream().anyMatch(Objects::isNull)) {
            throw new ExchangeRateProviderException("Resposta do provedor de câmbio incompleta");
        }
    }
}