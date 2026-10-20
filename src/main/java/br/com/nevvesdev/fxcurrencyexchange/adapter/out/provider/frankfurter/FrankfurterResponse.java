package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterResponse(String base, LocalDate date, Map<String, BigDecimal> rates) {
}