package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import br.com.nevvesdev.fxcurrencyexchange.application.model.ConversionResult;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionResponse(
        UUID id,
        String from,
        String to,
        String amount,
        String result,
        String rate,
        Instant rateQuotedAt,
        Instant convertedAt,
        String rateSource) {

    public static ConversionResponse from(Conversion conversion) {
        return of(conversion, null);
    }

    public static ConversionResponse from(ConversionResult result) {
        return of(result.conversion(), result.rateSource().name());
    }

    private static ConversionResponse of(Conversion conversion, String rateSource) {
        return new ConversionResponse(
                conversion.id(),
                conversion.source().currency().value(),
                conversion.result().currency().value(),
                conversion.source().amount().toPlainString(),
                conversion.result().amount().toPlainString(),
                conversion.rate().rate().toPlainString(),
                conversion.rate().quotedAt(),
                conversion.convertedAt(),
                rateSource);
    }
}