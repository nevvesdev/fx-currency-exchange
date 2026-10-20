# FX Rate & Currency Exchange API

[![CI](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml/badge.svg)](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml)

API REST de câmbio e taxas de moedas, construída com Java 21 e Spring Boot 4, pensada para casos de uso de fintechs e bancos digitais.

> **Status:** em desenvolvimento (Fase 0: scaffold do projeto).

## Objetivo

Consultar taxas de câmbio de um provedor externo, armazenar o histórico, guardar as taxas atuais em cache com Redis e atualizá-las periodicamente em um agendamento. As taxas são de referência e são atualizadas periodicamente, não tick a tick.

## Tecnologias

- Java 21
- Spring Boot 4.0.8 (Web MVC, Validation, Data JPA, Data Redis, Cache, Actuator)
- PostgreSQL 17 com Flyway
- Redis 7.4
- Maven

## Como rodar

Pré-requisitos: Java 21 e Docker.

```bash
docker-compose up -d
./mvnw spring-boot:run
```

Health check: `http://localhost:8080/actuator/health`

## Testes

```bash
./mvnw verify
```

## Roadmap

- [x] Fase 0: scaffold, infraestrutura local e CI
- [ ] Fase 1: domínio de câmbio
- [ ] Fase 2: adaptador do provedor externo
- [ ] Fase 3: persistência e histórico
- [ ] Fase 4: cache Redis (Cache-Aside)
- [ ] Fase 5: atualização automática das taxas
- [ ] Fase 6: API REST e documentação OpenAPI
- [ ] Fase 7: hardening, métricas e Dockerfile

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).