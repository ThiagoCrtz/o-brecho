package br.org.ang.brecho.domain;

import java.util.UUID;

public record Actor(UUID id, String nome, Role role) {
  public void requireManager() {
    if (role != Role.ADMINISTRADOR) throw RuleViolation.forbidden();
  }

  public void requireApproval() {
    if (role != Role.ADMINISTRADOR && role != Role.TRIAGEM) throw RuleViolation.forbidden();
  }

  public void requireSales() {
    if (role != Role.ADMINISTRADOR && role != Role.ATENDENTE) throw RuleViolation.forbidden();
  }
}
