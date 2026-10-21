package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;

import java.util.List;
import java.util.Objects;

public record RefreshReport(int requestedBases, List<CurrencyCode> failedBases, int ratesFetched, int ratesInserted) {

    public RefreshReport {
        Objects.requireNonNull(failedBases, "failedBases é obrigatório");
        if (requestedBases < 0 || ratesFetched < 0 || ratesInserted < 0) {
            throw new IllegalArgumentException("Contadores não podem ser negativos");
        }
        if (failedBases.size() > requestedBases) {
            throw new IllegalArgumentException("Falhas não podem exceder as moedas solicitadas");
        }
        failedBases = List.copyOf(failedBases);
    }

    public boolean isSuccess() {
        return failedBases.isEmpty();
    }

    public boolean isTotalFailure() {
        return requestedBases > 0 && failedBases.size() == requestedBases;
    }
}