package br.org.ang.brecho.web;

import br.org.ang.brecho.application.*;
import br.org.ang.brecho.domain.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class SaleController {
  private final SaleService sales;

  public SaleController(SaleService sales) {
    this.sales = sales;
  }

  @PostMapping("/vendas")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> sell(
      @AuthenticationPrincipal Actor actor,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody Commands.Sale c) {
    return sales.sell(actor, key, c);
  }

  @GetMapping("/vendas/{id}")
  public Map<String, Object> detail(@PathVariable UUID id) {
    return sales.detail(id);
  }

  @GetMapping("/vendas")
  public List<Map<String, Object>> list(
      @RequestParam LocalDate inicio,
      @RequestParam LocalDate fim,
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    return sales.list(inicio, fim, clienteId, pagina, tamanho);
  }

  @GetMapping("/relatorios")
  public Map<String, Object> report(@RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
    return sales.report(inicio, fim);
  }
}
