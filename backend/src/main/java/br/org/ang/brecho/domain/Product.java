package br.org.ang.brecho.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(UUID id, String status, BigDecimal price) {
  public void requireStatus(String expected) {
    if (!status.equals(expected))
      throw RuleViolation.conflict(
          "A peça não está mais na situação esperada. Atualize a consulta.");
  }
}
