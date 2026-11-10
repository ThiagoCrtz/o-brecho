package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.Commands.Staff;
import br.org.ang.brecho.application.port.IdentityRepository;
import br.org.ang.brecho.domain.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIdentityRepository implements IdentityRepository {
  private final JdbcTemplate db;
  private final RowMapper<StaffAccount> mapper =
      (r, n) ->
          new StaffAccount(
              r.getObject("id", UUID.class),
              r.getString("name"),
              r.getString("username"),
              r.getString("password_hash"),
              Role.valueOf(r.getString("role")),
              r.getBoolean("active"));

  public JdbcIdentityRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Optional<StaffAccount> byUsername(String username) {
    return db.query("SELECT * FROM staff WHERE username=?", mapper, username).stream().findFirst();
  }

  public Optional<Actor> byTokenHash(String hash) {
    return db
        .query(
            """
            SELECT s.* FROM staff s JOIN auth_session t ON t.staff_id=s.id
            WHERE t.token_hash=? AND t.expires_at>CURRENT_TIMESTAMP AND s.active=TRUE
            """,
            mapper,
            hash)
        .stream()
        .findFirst()
        .map(StaffAccount::actor);
  }

  public void create(UUID id, Staff data, String hash) {
    db.update(
        "INSERT INTO staff VALUES (?,?,?,?,?,?,?)",
        id,
        data.nome().trim(),
        data.usuario().trim().toLowerCase(Locale.ROOT),
        hash,
        data.perfil().name(),
        data.telefone(),
        data.ativo());
  }

  public StaffAccount lock(UUID id) {
    return db.query("SELECT * FROM staff WHERE id=? FOR UPDATE", mapper, id).stream()
        .findFirst()
        .orElseThrow(RuleViolation::missing);
  }

  public void update(UUID id, Staff data) {
    db.update(
        "UPDATE staff SET name=?,username=?,role=?,phone=?,active=? WHERE id=?",
        data.nome().trim(),
        data.usuario().trim().toLowerCase(Locale.ROOT),
        data.perfil().name(),
        data.telefone(),
        data.ativo(),
        id);
  }

  public void password(UUID id, String hash) {
    db.update("UPDATE staff SET password_hash=? WHERE id=?", hash, id);
  }

  public void revoke(UUID id) {
    db.update("DELETE FROM auth_session WHERE staff_id=?", id);
  }

  public void openSession(String hash, UUID id, OffsetDateTime expires) {
    db.update("DELETE FROM auth_session WHERE expires_at<CURRENT_TIMESTAMP");
    db.update("INSERT INTO auth_session VALUES (?,?,?)", hash, id, expires);
  }

  public void closeSession(String hash) {
    db.update("DELETE FROM auth_session WHERE token_hash=?", hash);
  }

  public List<Map<String, Object>> list(int page, int size) {
    return db.queryForList(
        "SELECT id,name,username,role,phone,active FROM staff ORDER BY name,id LIMIT ? OFFSET ?",
        size,
        page * size);
  }

  public void lockManagement() {
    db.queryForObject("SELECT id FROM management_lock WHERE id=1 FOR UPDATE", Integer.class);
  }

  public long count() {
    return db.queryForObject("SELECT count(*) FROM staff", Long.class);
  }
}
