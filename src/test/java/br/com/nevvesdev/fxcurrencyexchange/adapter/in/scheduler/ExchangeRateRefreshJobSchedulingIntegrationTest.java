package br.com.nevvesdev.fxcurrencyexchange.adapter.in.scheduler;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.RefreshExchangeRatesUseCase;
import br.com.nevvesdev.fxcurrencyexchange.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"fx.refresh.enabled=true", "fx.refresh.cron=-"})
class ExchangeRateRefreshJobSchedulingIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ExchangeRateRefreshJob job;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("deve executar o job sob lock distribuído registrado no PostgreSQL")
    void shouldRunUnderDistributedLock() {
        job.run();

        Integer locks = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM shedlock WHERE name = ?", Integer.class, ExchangeRateRefreshJob.LOCK_NAME);

        assertThat(locks).isEqualTo(1);
    }

    @TestConfiguration
    static class StubUseCaseConfig {

        @Bean
        @Primary
        RefreshExchangeRatesUseCase stubRefreshUseCase() {
            return bases -> new RefreshReport(bases.size(), List.of(), 0, 0);
        }
    }
}