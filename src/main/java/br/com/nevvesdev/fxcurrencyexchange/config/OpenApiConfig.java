package br.com.nevvesdev.fxcurrencyexchange.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI fxCurrencyExchangeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FX Rate & Currency Exchange API")
                .description("API REST de câmbio e taxas de moedas: consulta de taxas com cache, "
                        + "histórico e conversões com valores monetários de precisão exata.")
                .version("v1")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}