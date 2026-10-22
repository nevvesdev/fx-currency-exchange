package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web;

import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.ConversionListResponse;
import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.ConversionRequest;
import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.ConversionResponse;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConversionResult;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ConvertCurrencyCommand;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.ConvertCurrencyUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.QueryConversionsUseCase;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversions")
@Tag(name = "Conversões", description = "Conversão de valores entre moedas e histórico de conversões")
public class ConversionController {

    private final ConvertCurrencyUseCase convertCurrency;
    private final QueryConversionsUseCase queryConversions;

    public ConversionController(ConvertCurrencyUseCase convertCurrency, QueryConversionsUseCase queryConversions) {
        this.convertCurrency = convertCurrency;
        this.queryConversions = queryConversions;
    }

    @PostMapping
    @Operation(summary = "Converte um valor entre duas moedas e registra a conversão")
    public ResponseEntity<ConversionResponse> convert(@Valid @RequestBody ConversionRequest request) {
        ConversionResult result = convertCurrency.convert(new ConvertCurrencyCommand(
                CurrencyCode.of(request.from()), CurrencyCode.of(request.to()), request.amount()));
        ConversionResponse body = ConversionResponse.from(result);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma conversão pelo identificador")
    public ConversionResponse getById(@PathVariable("id") UUID id) {
        return ConversionResponse.from(queryConversions.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lista as conversões mais recentes (limite de 1 a 100, padrão 20)")
    public ConversionListResponse listRecent(
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ConversionListResponse.from(limit, queryConversions.listRecent(limit));
    }
}