package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConversionPersistenceAdapter implements ConversionRepository {

    private final ConversionJpaRepository jpaRepository;

    public ConversionPersistenceAdapter(ConversionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Conversion save(Conversion conversion) {
        Objects.requireNonNull(conversion, "conversion é obrigatória");
        jpaRepository.save(ConversionEntityMapper.toEntity(conversion));
        return conversion;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversion> findById(UUID id) {
        return jpaRepository.findById(id).map(ConversionEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversion> findRecent(int limit) {
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "convertedAt"));
        return jpaRepository.findAll(page).getContent().stream()
                .map(ConversionEntityMapper::toDomain)
                .toList();
    }
}