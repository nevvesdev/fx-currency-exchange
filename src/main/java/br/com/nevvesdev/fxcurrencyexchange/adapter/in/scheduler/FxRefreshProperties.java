package br.com.nevvesdev.fxcurrencyexchange.adapter.in.scheduler;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "fx.refresh")
public record FxRefreshProperties(List<String> bases) {

    public FxRefreshProperties {
        Objects.requireNonNull(bases, "bases é obrigatório");
        if (bases.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma moeda em fx.refresh.bases");
        }
        bases.forEach(CurrencyCode::of);
        bases = List.copyOf(bases);
    }

    public List<CurrencyCode> baseCurrencies() {
        return bases.stream().map(CurrencyCode::of).distinct().toList();
    }
}