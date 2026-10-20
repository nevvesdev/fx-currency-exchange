package br.com.nevvesdev.fxcurrencyexchange;

import br.com.nevvesdev.fxcurrencyexchange.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FxCurrencyExchangeApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    @DisplayName("deve carregar o contexto aplicando migrations e validando o schema")
    void contextLoads() {
    }
}