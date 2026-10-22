package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ConversionNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.QueryConversionsUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConversionQueryService implements QueryConversionsUseCase {

    private final ConversionRepository repository;

    public ConversionQueryService(ConversionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository é obrigatório");
    }

    @Override
    public Conversion getById(UUID id) {
        Objects.requireNonNull(id, "id é obrigatório");
        return repository.findById(id).orElseThrow(() -> new ConversionNotFoundException(id));
    }

    @Override
    public List<Conversion> listRecent(int limit) {
        return repository.findRecent(limit);
    }
}