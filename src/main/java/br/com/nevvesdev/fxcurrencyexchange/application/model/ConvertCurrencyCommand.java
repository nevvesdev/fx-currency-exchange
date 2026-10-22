package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.util.Objects;

public record ConvertCurrencyCommand(CurrencyCode from, CurrencyCode to, BigDecimal amount) {

    public ConvertCurrencyCommand {
        Objects.requireNonNull(from, "from é obrigatória");
        Objects.requireNonNull(to, "to é obrigatória");
        Objects.requireNonNull(amount, "amount é obrigatório");
    }
}