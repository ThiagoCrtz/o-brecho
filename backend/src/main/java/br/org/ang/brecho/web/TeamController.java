package br.org.ang.brecho.web;

import br.org.ang.brecho.application.*;
import br.org.ang.brecho.application.port.AuditRepository;
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
public class TeamController {
  private final TeamService team;
  private final AuditRepository audit;

  public TeamController(TeamService team, AuditRepository audit) {
    this.team = team;
    this.audit = audit;
  }

  @GetMapping("/equipe")
  public List<Map<String, Object>> list(
      @AuthenticationPrincipal Actor actor,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    return team.list(actor, pagina, tamanho);
  }

  @PostMapping("/equipe")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, UUID> create(
      @AuthenticationPrincipal Actor actor, @Valid @RequestBody Commands.CreateStaff c) {
    return Map.of("id", team.create(actor, c));
  }

  @PutMapping("/equipe/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Staff c) {
    team.update(actor, id, c);
  }

  @PostMapping("/equipe/{id}/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(
      @AuthenticationPrincipal Actor actor,
      @PathVariable UUID id,
      @Valid @RequestBody Commands.Password c) {
    team.resetPassword(actor, id, c);
  }

  @GetMapping("/auditoria")
  public List<Map<String, Object>> audit(
      @AuthenticationPrincipal Actor actor,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho) {
    actor.requireManager();
    return audit.list(pagina, tamanho);
  }
}
