package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    private static final String LATEST_PATH = "/latest";

    private final RestClient restClient;

    public FrankfurterExchangeRateProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
        Objects.requireNonNull(base, "base é obrigatória");
        Objects.requireNonNull(quote, "quote é obrigatória");
        String pair = base + "/" + quote;
        FrankfurterResponse response = retrieve(
                uri -> uri.path(LATEST_PATH)
                        .queryParam("from", base.value())
                        .queryParam("to", quote.value())
                        .build(),
                pair);
        return FrankfurterResponseMapper.toExchangeRates(response).stream()
                .filter(rate -> rate.quote().equals(quote))
                .findFirst()
                .orElseThrow(() -> new ExchangeRateNotFoundException(notFoundMessage(pair)));
    }

    @Override
    public List<ExchangeRate> fetchRates(CurrencyCode base) {
        Objects.requireNonNull(base, "base é obrigatória");
        FrankfurterResponse response = retrieve(
                uri -> uri.path(LATEST_PATH)
                        .queryParam("from", base.value())
                        .build(),
                base.value());
        return FrankfurterResponseMapper.toExchangeRates(response);
    }

    private FrankfurterResponse retrieve(Function<UriBuilder, URI> uriFunction, String description) {
        try {
            return restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404 || status.value() == 422,
                            (request, response) -> {
                                throw new ExchangeRateNotFoundException(notFoundMessage(description));
                            })
                    .body(FrankfurterResponse.class);
        } catch (RestClientException e) {
            throw new ExchangeRateProviderException("Falha ao consultar o provedor de câmbio", e);
        }
    }

    private static String notFoundMessage(String description) {
        return "Taxa não encontrada no provedor: " + description;
    }
}