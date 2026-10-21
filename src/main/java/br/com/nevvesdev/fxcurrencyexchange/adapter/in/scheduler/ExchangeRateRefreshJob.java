package br.com.nevvesdev.fxcurrencyexchange.adapter.in.scheduler;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.RefreshExchangeRatesUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fx.refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExchangeRateRefreshJob {

    public static final String LOCK_NAME = "fx-refresh-exchange-rates";

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateRefreshJob.class);
    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_PARTIAL = "partial";
    private static final String OUTCOME_FAILURE = "failure";

    private final RefreshExchangeRatesUseCase refreshUseCase;
    private final FxRefreshProperties properties;
    private final MeterRegistry meterRegistry;

    public ExchangeRateRefreshJob(
            RefreshExchangeRatesUseCase refreshUseCase,
            FxRefreshProperties properties,
            MeterRegistry meterRegistry) {
        this.refreshUseCase = refreshUseCase;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "${fx.refresh.cron}", zone = "${fx.refresh.zone}")
    @SchedulerLock(
            name = LOCK_NAME,
            lockAtMostFor = "${fx.refresh.lock-at-most-for}",
            lockAtLeastFor = "${fx.refresh.lock-at-least-for}")
    public void run() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = OUTCOME_FAILURE;
        try {
            RefreshReport report = refreshUseCase.refresh(properties.baseCurrencies());
            outcome = outcomeOf(report);
            meterRegistry.counter("fx.refresh.rates.inserted").increment(report.ratesInserted());
            log.info("Atualização de taxas concluída: resultado={}, moedas={}, falhas={}, taxas={}, novas={}",
                    outcome, report.requestedBases(), report.failedBases(), report.ratesFetched(),
                    report.ratesInserted());
        } finally {
            sample.stop(meterRegistry.timer("fx.refresh.duration", "outcome", outcome));
            meterRegistry.counter("fx.refresh.runs", "outcome", outcome).increment();
        }
    }

    private static String outcomeOf(RefreshReport report) {
        if (report.isSuccess()) {
            return OUTCOME_SUCCESS;
        }
        return report.isTotalFailure() ? OUTCOME_FAILURE : OUTCOME_PARTIAL;
    }
}