package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "fx.provider.circuit-breaker")
public record FxCircuitBreakerProperties(
        float failureRateThreshold,
        int slidingWindowSize,
        int minimumCalls,
        Duration waitInOpenState,
        int halfOpenCalls) {

    public FxCircuitBreakerProperties {
        if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
            throw new IllegalArgumentException("failureRateThreshold deve ser maior que 0 e no máximo 100");
        }
        if (slidingWindowSize < 1) {
            throw new IllegalArgumentException("slidingWindowSize deve ser pelo menos 1");
        }
        if (minimumCalls < 1) {
            throw new IllegalArgumentException("minimumCalls deve ser pelo menos 1");
        }
        Objects.requireNonNull(waitInOpenState, "waitInOpenState é obrigatório");
        if (waitInOpenState.isZero() || waitInOpenState.isNegative()) {
            throw new IllegalArgumentException("waitInOpenState deve ser maior que zero");
        }
        if (halfOpenCalls < 1) {
            throw new IllegalArgumentException("halfOpenCalls deve ser pelo menos 1");
        }
    }
}