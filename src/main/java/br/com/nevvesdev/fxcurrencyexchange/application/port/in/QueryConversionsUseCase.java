package br.com.nevvesdev.fxcurrencyexchange.application.port.in;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;

import java.util.List;
import java.util.UUID;

public interface QueryConversionsUseCase {

    Conversion getById(UUID id);

    List<Conversion> listRecent(int limit);
}