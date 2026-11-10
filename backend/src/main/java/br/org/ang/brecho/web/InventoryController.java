package br.org.ang.brecho.web;

import br.org.ang.brecho.application.*;
import br.org.ang.brecho.domain.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class InventoryController {
  private final InventoryService inventory;

  public InventoryController(InventoryService inventory) {
    this.inventory = inventory;
  }

  @GetMapping("/produtos")
  public List<Map<String, Object>> products(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    return inventory.products(status, pagina, tamanho);
  }

  @GetMapping("/doacoes")
  public List<Map<String, Object>> donations(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    return inventory.donations(pagina, tamanho);
  }

  @PostMapping("/doacoes")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> receive(
      @AuthenticationPrincipal Actor actor, @Valid @RequestBody Commands.Donation c) {
    return Map.of("id", inventory.receive(actor, c));
  }

  @PostMapping("/triagens/{id}/aprovar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void approve(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Approve c) {
    inventory.approve(actor, id, c);
  }

  @PostMapping("/triagens/{id}/descartar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reject(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Note c) {
    inventory.reject(actor, id, c);
  }

  @PostMapping("/triagens/{id}/reavaliar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reassess(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Note c) {
    inventory.reassess(actor, id, c);
  }

  @PatchMapping("/produtos/{id}/localizacao")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void move(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Stock c) {
    inventory.move(actor, id, c);
  }
}
