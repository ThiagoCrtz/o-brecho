package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.Commands.*;
import br.org.ang.brecho.application.port.DirectoryRepository;
import br.org.ang.brecho.domain.RuleViolation;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDirectoryRepository implements DirectoryRepository {
  private final JdbcTemplate db;

  public JdbcDirectoryRepository(JdbcTemplate db) {
    this.db = db;
  }

  public void createCustomer(UUID id, Customer c) {
    db.update(
        "INSERT INTO customer VALUES (?,?,?,?,?,TRUE)",
        id,
        c.nome().trim(),
        c.telefone(),
        c.email(),
        c.observacoes());
  }

  public void updateCustomer(UUID id, Customer c) {
    lockActiveCustomer(id);
    db.update(
        "UPDATE customer SET name=?,phone=?,email=?,notes=? WHERE id=?",
        c.nome().trim(),
        c.telefone(),
        c.email(),
        c.observacoes(),
        id);
  }

  public void archiveCustomer(UUID id) {
    lockActiveCustomer(id);
    db.update("UPDATE customer SET active=FALSE WHERE id=?", id);
  }

  public void lockActiveCustomer(UUID id) {
    var active =
        db.query(
            "SELECT active FROM customer WHERE id=? FOR UPDATE", (r, n) -> r.getBoolean(1), id);
    if (active.isEmpty()) throw RuleViolation.missing();
    RuleViolation.require(active.get(0), "Cliente inativo.");
  }

  public List<Map<String, Object>> customers(int page, int size) {
    return db.queryForList(
        "SELECT * FROM customer ORDER BY name,id LIMIT ? OFFSET ?", size, page * size);
  }

  public void createCategory(UUID id, Category c) {
    db.update("INSERT INTO category VALUES (?,?,?)", id, c.nome().trim(), c.descricao());
  }

  public List<Map<String, Object>> categories() {
    return db.queryForList("SELECT * FROM category ORDER BY name,id");
  }
}
