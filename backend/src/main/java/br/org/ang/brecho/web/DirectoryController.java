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
public class DirectoryController {
  private final DirectoryService directory;

  public DirectoryController(DirectoryService directory) {
    this.directory = directory;
  }

  @GetMapping("/clientes")
  public List<Map<String, Object>> customers(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    return directory.customers(pagina, tamanho);
  }

  @PostMapping("/clientes")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> create(
      @AuthenticationPrincipal Actor actor, @Valid @RequestBody Commands.Customer c) {
    return Map.of("id", directory.createCustomer(actor, c));
  }

  @PutMapping("/clientes/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Customer c) {
    directory.updateCustomer(actor, id, c);
  }

  @PostMapping("/clientes/{id}/arquivar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
    directory.archiveCustomer(actor, id);
  }

  @GetMapping("/categorias")
  public List<Map<String, Object>> categories() {
    return directory.categories();
  }

  @PostMapping("/categorias")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> category(
      @AuthenticationPrincipal Actor actor, @Valid @RequestBody Commands.Category c) {
    return Map.of("id", directory.createCategory(actor, c));
  }
}
