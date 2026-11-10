package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.port.AuditRepository;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditRepository implements AuditRepository {
  private final JdbcTemplate db;

  public JdbcAuditRepository(JdbcTemplate db) {
    this.db = db;
  }

  public void record(UUID actor, String action, UUID entity, String detail) {
    db.update(
        "INSERT INTO audit_event VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        actor,
        action,
        entity,
        OffsetDateTime.now(),
        detail);
  }

  public List<Map<String, Object>> list(int page, int size) {
    return db.queryForList(
        "SELECT * FROM audit_event ORDER BY occurred_at DESC,id LIMIT ? OFFSET ?",
        size,
        page * size);
  }
}
