# FX Rate & Currency Exchange API

[![CI](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml/badge.svg)](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml)

API REST de câmbio e taxas de moedas, construída com Java 21 e Spring Boot 4, pensada para casos de uso de fintechs e bancos digitais.

> **Status:** em desenvolvimento (Fase 0: scaffold do projeto).

## Objetivo

Consultar taxas de câmbio de um provedor externo, armazenar o histórico, guardar as taxas atuais em cache com Redis e atualizá-las periodicamente em um agendamento. As taxas são de referência e são atualizadas periodicamente, não tick a tick.

## Tecnologias

- Java 21
- Spring Boot 4.0.8
- PostgreSQL 17
- Flyway Migrations
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
Os testes de integração usam Testcontainers com PostgreSQL real, então o Docker precisa estar em execução.

```bash
./mvnw verify
```

## Decisões de design

- **`BigDecimal` para valores e taxas**: nunca `double`, evitando erros de ponto flutuante em cálculos financeiros.
- **Arredondamento `HALF_EVEN` (bancário)** na escala padrão de cada moeda conforme ISO 4217, evitando viés acumulado.
- **Domínio sem dependência de framework**: regras de câmbio em Java puro, testáveis sem subir contexto Spring.
- **Value objects imutáveis (`record`)** com invariantes validadas na construção: objeto inválido não existe.
- **Exceções de domínio com base comum**: a camada web as traduz para respostas HTTP sem que o domínio conheça HTTP.
- **Porta e adaptador para o provedor de taxas**: a aplicação depende da interface `ExchangeRateProvider`; o Frankfurter é só uma implementação trocável.
- **Mapeamento anticorrupção**: o JSON do provedor é convertido em objetos de domínio na borda, e dados inválidos viram falha do provedor, nunca estado inválido no domínio.
- **Falhas distintas**: taxa inexistente (`ExchangeRateNotFoundException`) e indisponibilidade do provedor (`ExchangeRateProviderException`) são tratadas de forma diferente.
- **Timeouts explícitos** de conexão e leitura, configuráveis por variável de ambiente.
- **Schema versionado com Flyway** e constraints no banco (formato da moeda, taxa positiva, moedas distintas); o Hibernate apenas valida o mapeamento.
- **Entidades JPA isoladas do domínio**, com mappers explícitos nos adaptadores de persistência.
- **Histórico de taxas idempotente**: chave única por par e instante de cotação com `INSERT ... ON CONFLICT DO NOTHING`, seguro sob concorrência.
- **Precisão numérica explícita**: taxas em `NUMERIC(40,20)`, valores monetários em `NUMERIC(28,4)` e instantes em `TIMESTAMPTZ`.
- **Auditabilidade**: cada conversão guarda a taxa aplicada e o instante da cotação.
- **Testes com PostgreSQL real** via Testcontainers, com container compartilhado para preservar o cache de contexto do Spring.

## Provedor de taxas

As taxas vêm do [Frankfurter](https://frankfurter.dev), API gratuita e sem chave. Usamos o endpoint `v1/latest`, que expõe taxas de referência publicadas uma vez por dia útil, portanto o `quotedAt` de cada taxa é o início do dia de referência em UTC.

| Variável | Padrão | Descrição |
|---|---|---|
| `FX_PROVIDER_BASE_URL` | `https://api.frankfurter.dev/v1` | URL base do provedor |
| `FX_PROVIDER_CONNECT_TIMEOUT` | `2s` | Timeout de conexão |
| `FX_PROVIDER_READ_TIMEOUT` | `5s` | Timeout de leitura |

Teste opcional contra a API real:

```bash
FX_LIVE_TESTS=true ./mvnw -B test -Dtest=FrankfurterExchangeRateProviderLiveTest
```

## Roadmap

- [x] Fase 0: scaffold, infraestrutura local e CI
- [x] Fase 1: domínio de câmbio
- [x] Fase 2: adaptador do provedor externo
- [x] Fase 3: persistência e histórico
- [ ] Fase 4: cache Redis (Cache-Aside)
- [ ] Fase 5: atualização automática das taxas
- [ ] Fase 6: API REST e documentação OpenAPI
- [ ] Fase 7: hardening, métricas e Dockerfile

## 👨‍💻 Desenvolvido por

João Victor · [GitHub](https://github.com/nevvesdev) · [LinkedIn](https://www.linkedin.com/in/nevvesdev/)

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).