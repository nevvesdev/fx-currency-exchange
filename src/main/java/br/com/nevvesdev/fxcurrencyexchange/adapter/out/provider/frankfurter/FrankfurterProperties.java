package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "fx.provider.frankfurter")
public record FrankfurterProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public FrankfurterProperties {
        Objects.requireNonNull(baseUrl, "baseUrl é obrigatória");
        Objects.requireNonNull(connectTimeout, "connectTimeout é obrigatório");
        Objects.requireNonNull(readTimeout, "readTimeout é obrigatório");
    }
}