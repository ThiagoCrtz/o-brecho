package br.org.ang.brecho.application;

import br.org.ang.brecho.application.Commands.*;
import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryRepository inventory;
  private final AuditRepository audit;

  public InventoryService(InventoryRepository inventory, AuditRepository audit) {
    this.inventory = inventory;
    this.audit = audit;
  }

  @Transactional
  public UUID receive(Actor actor, Donation command) {
    UUID id = UUID.randomUUID();
    inventory.receive(id, actor.id(), command);
    audit.record(actor.id(), "DOACAO_RECEBIDA", id, "Peças: " + command.pecas().size());
    return id;
  }

  @Transactional
  public void approve(Actor actor, UUID id, Approve command) {
    actor.requireApproval();
    inventory.lock(id).requireStatus("PENDENTE");
    RuleViolation.require(inventory.categoryExists(command.categoriaId()), "Categoria inválida.");
    Pricing.money(command.preco());
    inventory.approve(id, command);
    audit.record(actor.id(), "TRIAGEM_APROVADA", id, command.observacao());
  }

  @Transactional
  public void reject(Actor actor, UUID id, Note command) {
    inventory.lock(id).requireStatus("PENDENTE");
    inventory.changeStatus(id, "DESCARTADO", command.observacao());
    audit.record(actor.id(), "TRIAGEM_DESCARTADA", id, command.observacao());
  }

  @Transactional
  public void reassess(Actor actor, UUID id, Note command) {
    inventory.lock(id).requireStatus("DESCARTADO");
    inventory.changeStatus(id, "PENDENTE", command.observacao());
    audit.record(actor.id(), "TRIAGEM_REABERTA", id, command.observacao());
  }

  @Transactional
  public void move(Actor actor, UUID id, Stock command) {
    actor.requireApproval();
    inventory.lock(id).requireStatus("DISPONIVEL");
    inventory.move(id, command.localizacao());
    audit.record(actor.id(), "LOCALIZACAO_ALTERADA", id, command.localizacao());
  }

  public List<Map<String, Object>> products(String status, int page, int size) {
    RuleViolation.require(
        status == null
            || Set.of("PENDENTE", "DISPONIVEL", "DESCARTADO", "VENDIDO").contains(status),
        "Situação inválida.");
    return inventory.products(status, page, size);
  }

  public List<Map<String, Object>> donations(int page, int size) {
    return inventory.donations(page, size);
  }
}
