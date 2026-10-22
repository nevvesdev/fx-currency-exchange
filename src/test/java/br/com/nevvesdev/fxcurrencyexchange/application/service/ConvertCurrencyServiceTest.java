package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConversionResult;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConvertCurrencyCommand;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateSource;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConvertCurrencyServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode JPY = CurrencyCode.of("JPY");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-22T15:30:00.123456789Z"), ZoneOffset.UTC);

    private FakeConversionRepository conversions;
    private RateSource rateSource;
    private ConvertCurrencyService service;

    @BeforeEach
    void setUp() {
        conversions = new FakeConversionRepository();
        rateSource = RateSource.PROVIDER;
        GetExchangeRateUseCase rates = (base, quote) -> new ExchangeRateLookup(
                new ExchangeRate(base, quote, new BigDecimal("5.25"), QUOTED_AT), rateSource);
        service = new ConvertCurrencyService(rates, conversions, CLOCK);
    }

    @Test
    @DisplayName("deve converter, persistir e informar a origem da taxa")
    void shouldConvertAndPersist() {
        ConversionResult result = service.convert(command("USD", "BRL", "100.00"));

        Conversion conversion = result.conversion();
        assertThat(conversion.source()).isEqualTo(Money.of("100.00", "USD"));
        assertThat(conversion.result()).isEqualTo(Money.of("525.00", "BRL"));
        assertThat(conversion.rate().rate()).isEqualByComparingTo("5.25");
        assertThat(result.rateSource()).isEqualTo(RateSource.PROVIDER);
        assertThat(conversions.saved).containsExactly(conversion);
    }

    @Test
    @DisplayName("deve registrar o instante da conversão truncado em microssegundos")
    void shouldTruncateConversionTimestampToMicros() {
        ConversionResult result = service.convert(command("USD", "BRL", "10"));

        assertThat(result.conversion().convertedAt()).isEqualTo(Instant.parse("2026-10-22T15:30:00.123456Z"));
    }

    @Test
    @DisplayName("deve repassar a origem da taxa informada pela consulta")
    void shouldPassThroughRateSource() {
        rateSource = RateSource.DATABASE;

        ConversionResult result = service.convert(command("USD", "BRL", "10"));

        assertThat(result.rateSource()).isEqualTo(RateSource.DATABASE);
    }

    @Test
    @DisplayName("deve aceitar zeros à direita além das casas da moeda")
    void shouldAcceptTrailingZeros() {
        ConversionResult result = service.convert(command("USD", "BRL", "10.500"));

        assertThat(result.conversion().source().amount()).isEqualByComparingTo("10.50");
    }

    @Test
    @DisplayName("deve aceitar o valor máximo de 12 dígitos inteiros")
    void shouldAcceptMaximumIntegerDigits() {
        ConversionResult result = service.convert(command("USD", "BRL", "999999999999"));

        assertThat(result.conversion().source().amount()).isEqualByComparingTo("999999999999");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    @DisplayName("deve rejeitar valor zero ou negativo")
    void shouldRejectNonPositiveAmounts(String amount) {
        assertThatThrownBy(() -> service.convert(command("USD", "BRL", amount)))
                .isInstanceOf(InvalidAmountException.class);
        assertThat(conversions.saved).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000000000000", "1E+12", "1000000000000.50"})
    @DisplayName("deve rejeitar valor com mais de 12 dígitos inteiros")
    void shouldRejectTooManyIntegerDigits(String amount) {
        assertThatThrownBy(() -> service.convert(command("USD", "BRL", amount)))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("deve rejeitar casas decimais além das permitidas pela moeda de origem")
    void shouldRejectExcessFractionDigits() {
        assertThatThrownBy(() -> service.convert(command("USD", "BRL", "10.005")))
                .isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> service.convert(command("JPY", "BRL", "100.5")))
                .isInstanceOf(InvalidAmountException.class);
        assertThat(conversions.saved).isEmpty();
    }

    @Test
    @DisplayName("deve aceitar valor inteiro em moeda sem casas decimais")
    void shouldAcceptIntegerAmountInZeroFractionCurrency() {
        ConversionResult result = service.convert(command("JPY", "USD", "1000"));

        assertThat(result.conversion().source()).isEqualTo(Money.of("1000", "JPY"));
        assertThat(result.conversion().source().currency()).isEqualTo(JPY);
    }

    @Test
    @DisplayName("deve propagar a falha da consulta de taxa sem persistir")
    void shouldPropagateRateLookupFailure() {
        GetExchangeRateUseCase failing = (base, quote) -> {
            throw new ExchangeRateNotFoundException("par inexistente");
        };
        ConvertCurrencyService failingService = new ConvertCurrencyService(failing, conversions, CLOCK);

        assertThatThrownBy(() -> failingService.convert(command("USD", "BRL", "10")))
                .isInstanceOf(ExchangeRateNotFoundException.class);
        assertThat(conversions.saved).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar conversão entre moedas iguais")
    void shouldRejectSameCurrency() {
        GetExchangeRateUseCase strict = (base, quote) -> {
            throw new InvalidExchangeRateException("Moeda base e moeda cotada devem ser diferentes");
        };
        ConvertCurrencyService strictService = new ConvertCurrencyService(strict, conversions, CLOCK);

        assertThatThrownBy(() -> strictService.convert(command("USD", "USD", "10")))
                .isInstanceOf(InvalidExchangeRateException.class);
    }

    @Test
    @DisplayName("deve rejeitar comando nulo")
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.convert(null)).isInstanceOf(NullPointerException.class);
    }

    private static ConvertCurrencyCommand command(String from, String to, String amount) {
        return new ConvertCurrencyCommand(CurrencyCode.of(from), CurrencyCode.of(to), new BigDecimal(amount));
    }

    private static final class FakeConversionRepository implements ConversionRepository {

        private final List<Conversion> saved = new ArrayList<>();

        @Override
        public Conversion save(Conversion conversion) {
            saved.add(conversion);
            return conversion;
        }

        @Override
        public Optional<Conversion> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Conversion> findRecent(int limit) {
            throw new UnsupportedOperationException();
        }
    }
}