package br.org.ang.brecho.infrastructure;

import br.org.ang.brecho.application.port.SaleRepository;
import br.org.ang.brecho.domain.RuleViolation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSaleRepository implements SaleRepository {
  private final JdbcTemplate db;

  public JdbcSaleRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Optional<Existing> byRequestKey(UUID key) {
    return db
        .query(
            "SELECT id,staff_id,request_hash FROM sale WHERE request_key=?",
            (r, n) ->
                new Existing(
                    r.getObject(1, UUID.class), r.getObject(2, UUID.class), r.getString(3)),
            key)
        .stream()
        .findFirst();
  }

  public void create(
      UUID id, UUID customer, UUID actor, UUID key, String hash, BigDecimal total, String method) {
    OffsetDateTime now = OffsetDateTime.now();
    db.update(
        "INSERT INTO sale VALUES (?,?,?,?,?,?,?)", id, customer, actor, now, total, key, hash);
    db.update("INSERT INTO payment VALUES (?,?,?,?)", id, method, total, now);
  }

  public void item(UUID sale, UUID product, BigDecimal original, BigDecimal actual) {
    db.update("INSERT INTO sale_item VALUES (?,?,?,?)", sale, product, original, actual);
    db.update("UPDATE product SET status='VENDIDO' WHERE id=?", product);
  }

  public Map<String, Object> detail(UUID id) {
    var rows =
        db.queryForList(
            """
            SELECT s.id,s.customer_id,s.staff_id,s.created_at,s.total,p.method
            FROM sale s JOIN payment p ON p.sale_id=s.id WHERE s.id=?
            """,
            id);
    if (rows.isEmpty()) throw RuleViolation.missing();
    Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
    result.put(
        "itens",
        db.queryForList(
            """
            SELECT i.product_id,p.name,p.category_id,i.original_price,i.unit_price
            FROM sale_item i JOIN product p ON p.id=i.product_id WHERE i.sale_id=? ORDER BY i.product_id
            """,
            id));
    return result;
  }

  public List<Map<String, Object>> list(
      OffsetDateTime from, OffsetDateTime until, UUID customer, int page, int size) {
    String sql =
        "SELECT id,customer_id,staff_id,created_at,total FROM sale WHERE created_at>=? AND created_at<?";
    if (customer != null)
      return db.queryForList(
          sql + " AND customer_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",
          from,
          until,
          customer,
          size,
          page * size);
    return db.queryForList(
        sql + " ORDER BY created_at DESC,id LIMIT ? OFFSET ?", from, until, size, page * size);
  }

  public Map<String, Object> report(OffsetDateTime from, OffsetDateTime until) {
    Map<String, Object> result =
        new LinkedHashMap<>(
            db.queryForMap(
                """
            SELECT count(*) AS vendas,coalesce(sum(total),0) AS recebido FROM sale WHERE created_at>=? AND created_at<?
            """,
                from,
                until));
    result.put(
        "pecas",
        db.queryForObject(
            """
            SELECT count(*) FROM sale_item i JOIN sale s ON s.id=i.sale_id WHERE s.created_at>=? AND s.created_at<?
            """,
            Long.class,
            from,
            until));
    result.put(
        "porPagamento",
        db.queryForList(
            """
            SELECT p.method, count(*) AS vendas, sum(p.amount) AS recebido FROM payment p
            JOIN sale s ON s.id=p.sale_id WHERE s.created_at>=? AND s.created_at<? GROUP BY p.method ORDER BY p.method
            """,
            from,
            until));
    result.put(
        "porCategoria",
        db.queryForList(
            """
            SELECT c.id,c.name,count(*) AS pecas,sum(i.unit_price) AS recebido FROM sale_item i
            JOIN sale s ON s.id=i.sale_id JOIN product p ON p.id=i.product_id JOIN category c ON c.id=p.category_id
            WHERE s.created_at>=? AND s.created_at<? GROUP BY c.id,c.name ORDER BY c.name
            """,
            from,
            until));
    return result;
  }
}
