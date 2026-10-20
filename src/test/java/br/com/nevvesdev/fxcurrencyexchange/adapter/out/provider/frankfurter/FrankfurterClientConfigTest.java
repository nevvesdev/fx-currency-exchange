package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FrankfurterClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FrankfurterClientConfig.class);

    @Test
    @DisplayName("deve ligar as propriedades e criar o RestClient do provedor")
    void shouldBindPropertiesAndCreateRestClient() {
        contextRunner
                .withPropertyValues(
                        "fx.provider.frankfurter.base-url=http://localhost:9999/v1",
                        "fx.provider.frankfurter.connect-timeout=1s",
                        "fx.provider.frankfurter.read-timeout=3s")
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    assertThat(context).hasSingleBean(FrankfurterProperties.class);

                    FrankfurterProperties properties = context.getBean(FrankfurterProperties.class);
                    assertThat(properties.baseUrl()).isEqualTo("http://localhost:9999/v1");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(3));
                });
    }
}