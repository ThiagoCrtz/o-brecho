package br.org.ang.brecho.application;

import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
  private final IdentityRepository identities;
  private final AuthService auth;
  private final AuditRepository audit;

  public TeamService(IdentityRepository identities, AuthService auth, AuditRepository audit) {
    this.identities = identities;
    this.auth = auth;
    this.audit = audit;
  }

  private void verifyManager(Actor actor) {
    identities.lockManagement();
    StaffAccount current = identities.lock(actor.id());
    if (!current.active()) throw RuleViolation.forbidden();
    current.actor().requireManager();
  }

  public List<Map<String, Object>> list(Actor actor, int page, int size) {
    actor.requireManager();
    return identities.list(page, size);
  }

  @Transactional
  public UUID create(Actor actor, Commands.CreateStaff c) {
    verifyManager(actor);
    UUID id = UUID.randomUUID();
    identities.create(id, c.dados(), auth.encodePassword(c.senha()));
    audit.record(actor.id(), "USUARIO_CRIADO", id, c.dados().perfil().name());
    return id;
  }

  @Transactional
  public void update(Actor actor, UUID id, Commands.Staff c) {
    verifyManager(actor);
    identities.lock(id);
    RuleViolation.require(
        !actor.id().equals(id) || (c.ativo() && c.perfil() == Role.ADMINISTRADOR),
        "Você não pode desativar sua conta nem retirar seu próprio perfil de administrador.");
    identities.update(id, c);
    identities.revoke(id);
    audit.record(actor.id(), "USUARIO_ALTERADO", id, c.perfil().name() + "; ativo=" + c.ativo());
  }

  @Transactional
  public void resetPassword(Actor actor, UUID id, Commands.Password c) {
    verifyManager(actor);
    identities.lock(id);
    identities.password(id, auth.encodePassword(c.senha()));
    identities.revoke(id);
    audit.record(actor.id(), "SENHA_REDEFINIDA", id, "Sessões revogadas.");
  }
}
