package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RateHistory;

import java.time.LocalDate;
import java.util.List;

public record RateHistoryResponse(
        String base, String quote, LocalDate from, LocalDate to, List<RatePointResponse> rates) {

    public static RateHistoryResponse from(RateHistory history) {
        return new RateHistoryResponse(
                history.base().value(),
                history.quote().value(),
                history.from(),
                history.to(),
                history.rates().stream().map(RatePointResponse::from).toList());
    }
}