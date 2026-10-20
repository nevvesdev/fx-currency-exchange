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

## Decisões de design

- **`BigDecimal` para valores e taxas**: nunca `double`, evitando erros de ponto flutuante em cálculos financeiros.
- **Arredondamento `HALF_EVEN` (bancário)** na escala padrão de cada moeda conforme ISO 4217, evitando viés acumulado.
- **Domínio sem dependência de framework**: regras de câmbio em Java puro, testáveis sem subir contexto Spring.
- **Value objects imutáveis (`record`)** com invariantes validadas na construção: objeto inválido não existe.
- **Exceções de domínio com base comum**: a camada web as traduz para respostas HTTP sem que o domínio conheça HTTP.

## Roadmap

- [x] Fase 0: scaffold, infraestrutura local e CI
- [x] Fase 1: domínio de câmbio
- [ ] Fase 2: adaptador do provedor externo
- [ ] Fase 3: persistência e histórico
- [ ] Fase 4: cache Redis (Cache-Aside)
- [ ] Fase 5: atualização automática das taxas
- [ ] Fase 6: API REST e documentação OpenAPI
- [ ] Fase 7: hardening, métricas e Dockerfile

## 👨‍💻 Desenvolvido por

João Victor · [GitHub](https://github.com/nevvesdev) · [LinkedIn](https://www.linkedin.com/in/nevvesdev/)

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).