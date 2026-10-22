package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web;

import br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto.ApiFieldError;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ConversionNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.InvalidHistoryRangeException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage());
    }

    @ExceptionHandler(InvalidHistoryRangeException.class)
    public ProblemDetail handleInvalidHistoryRange(InvalidHistoryRangeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage());
    }

    @ExceptionHandler({ExchangeRateNotFoundException.class, ConversionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
    }

    @ExceptionHandler(ExchangeRateProviderException.class)
    public ProblemDetail handleProviderFailure(ExchangeRateProviderException exception) {
        log.warn("Provedor de câmbio indisponível: {}", exception.getMessage());
        return problem(
                HttpStatus.BAD_GATEWAY,
                "Provedor de câmbio indisponível",
                "Não foi possível obter a taxa no provedor externo e não há taxa anterior disponível.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Erro inesperado ao processar a requisição", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Um ou mais campos são inválidos");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}