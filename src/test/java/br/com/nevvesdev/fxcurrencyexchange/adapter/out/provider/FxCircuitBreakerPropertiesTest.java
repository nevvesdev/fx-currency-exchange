package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FxCircuitBreakerPropertiesTest {

    private static final Duration WAIT = Duration.ofSeconds(30);

    @Test
    @DisplayName("deve aceitar configuração válida")
    void shouldAcceptValidConfiguration() {
        FxCircuitBreakerProperties properties = new FxCircuitBreakerProperties(50f, 10, 5, WAIT, 3);

        assertThat(properties.slidingWindowSize()).isEqualTo(10);
        assertThat(properties.waitInOpenState()).isEqualTo(WAIT);
    }

    @Test
    @DisplayName("deve rejeitar limite de falhas fora do intervalo")
    void shouldRejectInvalidFailureRate() {
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(0f, 10, 5, WAIT, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(100.1f, 10, 5, WAIT, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve rejeitar janela, chamadas mínimas e chamadas em meio aberto menores que um")
    void shouldRejectNonPositiveCounters() {
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 0, 5, WAIT, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 10, 0, WAIT, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 10, 5, WAIT, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve rejeitar tempo de espera nulo, zero ou negativo")
    void shouldRejectInvalidWait() {
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 10, 5, null, 3))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 10, 5, Duration.ZERO, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxCircuitBreakerProperties(50f, 10, 5, Duration.ofSeconds(-1), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve ligar as propriedades a partir da configuração")
    void shouldBindFromConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class)
                .withPropertyValues(
                        "fx.provider.circuit-breaker.failure-rate-threshold=60",
                        "fx.provider.circuit-breaker.sliding-window-size=20",
                        "fx.provider.circuit-breaker.minimum-calls=8",
                        "fx.provider.circuit-breaker.wait-in-open-state=45s",
                        "fx.provider.circuit-breaker.half-open-calls=2")
                .run(context -> {
                    FxCircuitBreakerProperties properties = context.getBean(FxCircuitBreakerProperties.class);
                    assertThat(properties.failureRateThreshold()).isEqualTo(60f);
                    assertThat(properties.slidingWindowSize()).isEqualTo(20);
                    assertThat(properties.minimumCalls()).isEqualTo(8);
                    assertThat(properties.waitInOpenState()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(properties.halfOpenCalls()).isEqualTo(2);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FxCircuitBreakerProperties.class)
    static class PropertiesConfig {
    }
}