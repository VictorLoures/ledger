package dev.victorloures.ledger.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(JobNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), OffsetDateTime.now(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Erro de validação", OffsetDateTime.now(), violations));
    }

    // Rede de segurança: qualquer exceção não mapeada explicitamente ainda
    // sai no mesmo formato padronizado, nunca como o Whitelabel Error Page
    // (ou pior, um stacktrace cru) que o Spring devolveria por padrão — é
    // exatamente o problema demonstrado ao vivo no Módulo 3, generalizado
    // agora pra QUALQUER exceção, não só JobNotFoundException.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno", OffsetDateTime.now(), null));
    }

    // errors vem null pra erros que não são de validação de campo — mesmo
    // formato de envelope pra qualquer resposta de erro da API, com ou sem
    // detalhamento por campo.
    public record ErrorResponse(int status, String message, OffsetDateTime timestamp, List<FieldViolation> errors) {
        public record FieldViolation(String field, String message) {
        }
    }
}
