package br.org.ang.brecho.application;

import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectoryService {
  private final DirectoryRepository directory;
  private final AuditRepository audit;

  public DirectoryService(DirectoryRepository directory, AuditRepository audit) {
    this.directory = directory;
    this.audit = audit;
  }

  @Transactional
  public UUID createCustomer(Actor actor, Commands.Customer c) {
    UUID id = UUID.randomUUID();
    directory.createCustomer(id, c);
    audit.record(actor.id(), "CLIENTE_CRIADO", id, "");
    return id;
  }

  @Transactional
  public void updateCustomer(Actor actor, UUID id, Commands.Customer c) {
    directory.updateCustomer(id, c);
    audit.record(actor.id(), "CLIENTE_ALTERADO", id, "");
  }

  @Transactional
  public void archiveCustomer(Actor actor, UUID id) {
    actor.requireManager();
    directory.archiveCustomer(id);
    audit.record(actor.id(), "CLIENTE_ARQUIVADO", id, "Histórico preservado.");
  }

  @Transactional
  public UUID createCategory(Actor actor, Commands.Category c) {
    actor.requireManager();
    UUID id = UUID.randomUUID();
    directory.createCategory(id, c);
    audit.record(actor.id(), "CATEGORIA_CRIADA", id, "");
    return id;
  }

  public List<Map<String, Object>> customers(int page, int size) {
    return directory.customers(page, size);
  }

  public List<Map<String, Object>> categories() {
    return directory.categories();
  }
}
