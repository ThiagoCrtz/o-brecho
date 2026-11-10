package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.AuthService;
import br.org.ang.brecho.application.Commands.Staff;
import br.org.ang.brecho.application.port.IdentityRepository;
import br.org.ang.brecho.domain.Role;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrap implements ApplicationRunner {
  private final IdentityRepository identities;
  private final AuthService auth;
  private final String username, password;

  public Bootstrap(
      IdentityRepository identities,
      AuthService auth,
      @Value("${app.bootstrap.username:}") String username,
      @Value("${app.bootstrap.password:}") String password) {
    this.identities = identities;
    this.auth = auth;
    this.username = username;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    identities.lockManagement();
    if (identities.count() != 0 || username.isBlank()) return;
    if (!username.matches("[a-zA-Z0-9._-]{3,60}"))
      throw new IllegalArgumentException("BOOTSTRAP_USERNAME inválido.");
    identities.create(
        UUID.randomUUID(),
        new Staff("Administrador", username, Role.ADMINISTRADOR, "", true),
        auth.encodePassword(password));
  }
}
