package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversionJpaRepository extends JpaRepository<ConversionEntity, UUID> {
}