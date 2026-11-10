package br.org.ang.brecho.web;

import br.org.ang.brecho.domain.RuleViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errors {
  private static final Logger log = LoggerFactory.getLogger(Errors.class);

  @ExceptionHandler(RuleViolation.class)
  ResponseEntity<ProblemDetail> rule(RuleViolation e) {
    return response(e.status(), e.getMessage());
  }

  @ExceptionHandler({
    BindException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingRequestHeaderException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ProblemDetail> validation(Exception e) {
    return response(400, "Dados inválidos. Confira os campos e o formato da solicitação.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> conflict(Exception e) {
    return response(409, "Operação em conflito com um registro existente.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unknown(Exception e) {
    if (e instanceof org.springframework.web.ErrorResponse error)
      return response(error.getStatusCode().value(), "Solicitação não suportada.");
    String reference = java.util.UUID.randomUUID().toString();
    log.error("Falha interna referência={} tipo={}", reference, e.getClass().getName());
    return response(500, "Falha interna. Referência: " + reference);
  }

  private ResponseEntity<ProblemDetail> response(int status, String message) {
    return ResponseEntity.status(status)
        .body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message));
  }
}
