package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web;

import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.ExchangeRateResponse;
import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.RateHistoryResponse;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetRateHistoryUseCase;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Taxas de câmbio", description = "Consulta de taxas atuais e histórico")
public class RateController {

    private final GetExchangeRateUseCase getExchangeRate;
    private final GetRateHistoryUseCase getRateHistory;

    public RateController(GetExchangeRateUseCase getExchangeRate, GetRateHistoryUseCase getRateHistory) {
        this.getExchangeRate = getExchangeRate;
        this.getRateHistory = getRateHistory;
    }

    @GetMapping("/{base}/{quote}")
    @Operation(summary = "Consulta a taxa atual de um par de moedas e a origem do dado")
    public ExchangeRateResponse getRate(
            @PathVariable("base") String base, @PathVariable("quote") String quote) {
        return ExchangeRateResponse.from(
                getExchangeRate.getRate(CurrencyCode.of(base), CurrencyCode.of(quote)));
    }

    @GetMapping("/{base}/{quote}/history")
    @Operation(summary = "Consulta o histórico de taxas de um par (padrão: últimos 30 dias, máximo: 366)")
    public RateHistoryResponse getHistory(
            @PathVariable("base") String base,
            @PathVariable("quote") String quote,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return RateHistoryResponse.from(
                getRateHistory.getHistory(CurrencyCode.of(base), CurrencyCode.of(quote), from, to));
    }
}