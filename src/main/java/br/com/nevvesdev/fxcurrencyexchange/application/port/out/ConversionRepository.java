package br.com.nevvesdev.fxcurrencyexchange.application.port.out;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversionRepository {

    Conversion save(Conversion conversion);

    Optional<Conversion> findById(UUID id);

    List<Conversion> findRecent(int limit);
}