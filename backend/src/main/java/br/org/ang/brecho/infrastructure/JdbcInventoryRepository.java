package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.Commands.*;
import br.org.ang.brecho.application.port.InventoryRepository;
import br.org.ang.brecho.domain.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInventoryRepository implements InventoryRepository {
  private final JdbcTemplate db;

  public JdbcInventoryRepository(JdbcTemplate db) {
    this.db = db;
  }

  public void receive(UUID id, UUID actor, Donation c) {
    db.update(
        "INSERT INTO donation VALUES (?,?,?,?,?)",
        id,
        c.origem().trim(),
        c.observacao(),
        OffsetDateTime.now(),
        actor);
    for (Piece p : c.pecas())
      db.update(
          """
            INSERT INTO product(id,donation_id,name,description,size,brand,condition,status,location,notes)
            VALUES (?,?,?,?,?,?,?,'PENDENTE','Mesa de Triagem',?)
            """,
          UUID.randomUUID(),
          id,
          p.nome().trim(),
          p.descricao(),
          p.tamanho(),
          p.marca(),
          p.estado().name(),
          c.observacao());
  }

  public Product lock(UUID id) {
    return db
        .query(
            "SELECT id,status,price FROM product WHERE id=? FOR UPDATE",
            (r, n) ->
                new Product(
                    r.getObject("id", UUID.class), r.getString("status"), r.getBigDecimal("price")),
            id)
        .stream()
        .findFirst()
        .orElseThrow(RuleViolation::missing);
  }

  public boolean categoryExists(UUID id) {
    return db.queryForObject("SELECT count(*) FROM category WHERE id=?", Integer.class, id) > 0;
  }

  public void approve(UUID id, Approve c) {
    db.update(
        "UPDATE product SET status='DISPONIVEL',category_id=?,price=?,location=?,notes=? WHERE id=?",
        c.categoriaId(),
        c.preco(),
        c.localizacao(),
        c.observacao(),
        id);
  }

  public void changeStatus(UUID id, String status, String note) {
    db.update("UPDATE product SET status=?,notes=? WHERE id=?", status, note, id);
  }

  public void move(UUID id, String location) {
    db.update("UPDATE product SET location=? WHERE id=?", location, id);
  }

  public List<Map<String, Object>> products(String status, int page, int size) {
    if (status == null)
      return db.queryForList(
          "SELECT * FROM product ORDER BY id LIMIT ? OFFSET ?", size, page * size);
    return db.queryForList(
        "SELECT * FROM product WHERE status=? ORDER BY id LIMIT ? OFFSET ?",
        status,
        size,
        page * size);
  }

  public List<Map<String, Object>> donations(int page, int size) {
    return db.queryForList(
        "SELECT * FROM donation ORDER BY received_at DESC,id LIMIT ? OFFSET ?", size, page * size);
  }
}
