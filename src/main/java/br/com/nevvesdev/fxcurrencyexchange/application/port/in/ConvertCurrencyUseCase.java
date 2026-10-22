package br.com.nevvesdev.fxcurrencyexchange.application.port.in;

import br.com.nevvesdev.fxcurrencyexchange.application.model.ConversionResult;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConvertCurrencyCommand;

public interface ConvertCurrencyUseCase {

    ConversionResult convert(ConvertCurrencyCommand command);
}