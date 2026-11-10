package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.port.LoginThrottle;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcLoginThrottle implements LoginThrottle {
  private final JdbcTemplate db;

  public JdbcLoginThrottle(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional
  public boolean allow(String key, int limit) {
    OffsetDateTime now = OffsetDateTime.now(), expiry = now.plusMinutes(15);
    db.update("DELETE FROM login_bucket WHERE expires_at<?", now.minusDays(1));
    db.update("INSERT INTO login_bucket VALUES (?,0,?) ON CONFLICT DO NOTHING", key, expiry);
    db.update(
        """
            UPDATE login_bucket SET attempts=CASE WHEN expires_at<=? THEN 1 ELSE attempts+1 END,
            expires_at=CASE WHEN expires_at<=? THEN ? ELSE expires_at END WHERE bucket_key=?
            """,
        now,
        now,
        expiry,
        key);
    return db.queryForObject(
            "SELECT attempts FROM login_bucket WHERE bucket_key=?", Integer.class, key)
        <= limit;
  }
}
