package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;

import java.util.List;

public record ConversionListResponse(int limit, List<ConversionResponse> items) {

    public static ConversionListResponse from(int limit, List<Conversion> conversions) {
        return new ConversionListResponse(limit, conversions.stream().map(ConversionResponse::from).toList());
    }
}