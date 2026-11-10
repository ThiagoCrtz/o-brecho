package br.org.ang.brecho.domain;

public class RuleViolation extends RuntimeException {
  private final int status;

  public RuleViolation(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }

  public static RuleViolation forbidden() {
    return new RuleViolation(403, "Seu perfil não permite esta operação.");
  }

  public static RuleViolation missing() {
    return new RuleViolation(404, "Registro não encontrado.");
  }

  public static RuleViolation conflict(String message) {
    return new RuleViolation(409, message);
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new RuleViolation(422, message);
  }
}
