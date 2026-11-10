package br.org.ang.brecho.application;

import br.org.ang.brecho.application.port.IdentityRepository;
import br.org.ang.brecho.domain.*;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionIssuer {
  private final IdentityRepository identities;

  public SessionIssuer(IdentityRepository identities) {
    this.identities = identities;
  }

  @Transactional
  public Actor issue(StaffAccount verified, String tokenHash, OffsetDateTime expires) {
    // A redefinição de senha e a emissão de sessão disputam o mesmo bloqueio.
    StaffAccount current = identities.lock(verified.id());
    if (!current.active() || !current.passwordHash().equals(verified.passwordHash()))
      throw new RuleViolation(401, "Usuário ou senha inválidos.");
    identities.openSession(tokenHash, current.id(), expires);
    return current.actor();
  }
}
