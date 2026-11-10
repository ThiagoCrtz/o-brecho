package br.org.ang.brecho.domain;

import java.util.UUID;

public record StaffAccount(
    UUID id, String name, String username, String passwordHash, Role role, boolean active) {
  public Actor actor() {
    return new Actor(id, name, role);
  }
}
