package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FxCachePropertiesTest {

    @Test
    @DisplayName("deve aceitar TTL positivo")
    void shouldAcceptPositiveTtl() {
        assertThat(new FxCacheProperties(Duration.ofMinutes(5)).rateTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("deve rejeitar TTL nulo")
    void shouldRejectNullTtl() {
        assertThatThrownBy(() -> new FxCacheProperties(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1S"})
    @DisplayName("deve rejeitar TTL zero ou negativo")
    void shouldRejectNonPositiveTtl(String ttl) {
        assertThatThrownBy(() -> new FxCacheProperties(Duration.parse(ttl)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve ligar o TTL a partir da configuração")
    void shouldBindTtlFromConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(RedisCacheConfig.class)
                .withPropertyValues("fx.cache.rate-ttl=2m")
                .run(context -> assertThat(context.getBean(FxCacheProperties.class).rateTtl())
                        .isEqualTo(Duration.ofMinutes(2)));
    }
}