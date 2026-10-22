# FX Rate & Currency Exchange API

[![CI](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml/badge.svg)](https://github.com/nevvesdev/fx-currency-exchange/actions/workflows/ci.yml)

API REST de câmbio e taxas de moedas, construída com Java 21 e Spring Boot 4, pensada para casos de uso de fintechs e bancos digitais.

> **Status:** em desenvolvimento.

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
- **Entgit commit -m "test: cobre o fluxo Cache-Aside e os cenários de falha do provedor"idades JPA isoladas do domínio**, com mappers explícitos nos adaptadores de persistência.
- **Histórico de taxas idempotente**: chave única por par e instante de cotação com `INSERT ... ON CONFLICT DO NOTHING`, seguro sob concorrência.
- **Precisão numérica explícita**: taxas em `NUMERIC(40,20)`, valores monetários em `NUMERIC(28,4)` e instantes em `TIMESTAMPTZ`.
- **Auditabilidade**: cada conversão guarda a taxa aplicada e o instante da cotação.
- **Testes com PostgreSQL real** via Testcontainers, com container compartilhado para preservar o cache de contexto do Spring.
- **Cache-Aside com porta e adaptador próprios** (sem `@Cacheable`): controle total sobre chave, TTL, formato e política de falha.
- **Degradação graciosa**: Redis indisponível ou valor corrompido viram miss; a API segue funcionando pelo provedor, apenas mais lenta.
- **Fallback para a última taxa do banco** quando o provedor falha, com a origem do dado (`CACHE`, `PROVIDER` ou `DATABASE`) explícita no resultado e sem cachear dado antigo.
- **Chaves versionadas** (`fx:rate:v1:USD:BRL`) e gravação atômica com TTL.
- **Timeouts curtos do Redis** e Redis fora do health agregado, para que a queda do cache não retire a instância do balanceador.
- **Camada de aplicação sem Spring**: o caso de uso é Java puro, testado com fakes em memória.
- **Atualização agendada com lock distribuído (ShedLock sobre PostgreSQL)**: com várias instâncias, apenas uma executa o job por ciclo, usando o relógio do banco.
- **Tolerância a falhas por moeda base**: a falha de uma moeda não interrompe as demais; o relatório da execução classifica o ciclo como `success`, `partial` ou `failure`.
- **Gravação em lote no cache que aborta na primeira falha**: com o Redis indisponível, o ciclo paga um único timeout em vez de um por taxa.
- **Métricas do job** (`fx.refresh.runs`, `fx.refresh.duration`, `fx.refresh.rates.inserted`) expostas via Micrometer.
- **Job desligável por configuração**, o que mantém os testes de integração determinísticos.
- **Controllers finos**: a API só traduz HTTP para casos de uso em Java puro e de volta; regras de negócio ficam na aplicação e no domínio.
- **Valores e taxas como string decimal** nas respostas, evitando perda de precisão em clientes que leem JSON como `double`.
- **Dinheiro nunca é arredondado em silêncio**: valor com mais casas decimais que a moeda permite é rejeitado, e há limite de 12 dígitos inteiros.
- **Erros padronizados com `ProblemDetail` (RFC 7807)**, com lista de erros por campo na validação e sem vazar detalhes internos nos erros inesperados.
- **Origem do dado explícita** (`CACHE`, `PROVIDER`, `DATABASE`) em cada consulta de taxa.
- **Testes de ponta a ponta** com PostgreSQL real, provedor e cache falsos, cobrindo caminhos felizes e de erro.

## API

Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`
Especificação OpenAPI: `http://localhost:8080/v3/api-docs`

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/v1/rates/{base}/{quote}` | Taxa atual e origem do dado (`CACHE`, `PROVIDER` ou `DATABASE`) |
| GET | `/api/v1/rates/{base}/{quote}/history?from=&to=` | Histórico por datas (padrão: 30 dias; máximo: 366) |
| POST | `/api/v1/conversions` | Converte um valor e registra a conversão |
| GET | `/api/v1/conversions/{id}` | Busca uma conversão |
| GET | `/api/v1/conversions?limit=` | Conversões mais recentes (1 a 100) |

Exemplos:

    curl http://localhost:8080/api/v1/rates/USD/BRL

    curl -X POST http://localhost:8080/api/v1/conversions \
      -H "Content-Type: application/json" \
      -d '{"from":"USD","to":"BRL","amount":100.00}'

Valores e taxas são retornados como **strings decimais** (`"525.00"`), para não perder precisão em clientes que leem JSON como `double`. Erros seguem o formato `application/problem+json` (RFC 7807):

    {
      "title": "Requisição inválida",
      "status": 400,
      "detail": "O valor possui mais casas decimais do que a moeda USD permite (2)"
    }

## Provedor de taxas

As taxas vêm do [Frankfurter](https://frankfurter.dev), API gratuita e sem chave. Usamos o endpoint `v1/latest`, que expõe taxas de referência publicadas uma vez por dia útil, portanto o `quotedAt` de cada taxa é o início do dia de referência em UTC.

| Variável | Padrão | Descrição |
|---|---|---|
| `FX_PROVIDER_BASE_URL` | `https://api.frankfurter.dev/v1` | URL base do provedor |
| `FX_PROVIDER_CONNECT_TIMEOUT` | `2s` | Timeout de conexão |
| `FX_PROVIDER_READ_TIMEOUT` | `5s` | Timeout de leitura |
| `FX_CACHE_RATE_TTL` | `10m` | TTL das taxas no cache Redis |
| `FX_REFRESH_ENABLED` | `true` | Liga ou desliga o job de atualização automática |
| `FX_REFRESH_BASES` | `USD,EUR,BRL` | Moedas base atualizadas a cada ciclo |
| `FX_REFRESH_CRON` | `0 */5 * * * *` | Agenda do job (cron de 6 campos) |
| `FX_REFRESH_ZONE` | `UTC` | Fuso horário do cron |
| `FX_REFRESH_LOCK_AT_MOST_FOR` | `PT4M` | Tempo máximo do lock distribuído |
| `FX_REFRESH_LOCK_AT_LEAST_FOR` | `PT30S` | Tempo mínimo do lock distribuído |

Teste opcional contra a API real:

```bash
FX_LIVE_TESTS=true ./mvnw -B test -Dtest=FrankfurterExchangeRateProviderLiveTest
```

## Roadmap

- [x] Fase 0: scaffold, infraestrutura local e CI
- [x] Fase 1: domínio de câmbio
- [x] Fase 2: adaptador do provedor externo
- [x] Fase 3: persistência e histórico
- [x] Fase 4: cache Redis (Cache-Aside)
- [x] Fase 5: atualização automática das taxas
- [x] Fase 6: API REST e documentação OpenAPI
- [ ] Fase 7: hardening, métricas e Dockerfile

## 👨‍💻 Desenvolvido por

João Victor · [GitHub](https://github.com/nevvesdev) · [LinkedIn](https://www.linkedin.com/in/nevvesdev/)

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).