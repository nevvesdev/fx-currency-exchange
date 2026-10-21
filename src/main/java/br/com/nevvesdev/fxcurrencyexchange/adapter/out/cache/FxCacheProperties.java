package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "fx.cache")
public record FxCacheProperties(Duration rateTtl) {

    public FxCacheProperties {
        Objects.requireNonNull(rateTtl, "rateTtl é obrigatório");
        if (rateTtl.isZero() || rateTtl.isNegative()) {
            throw new IllegalArgumentException("rateTtl deve ser maior que zero");
        }
    }
}