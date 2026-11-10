package br.org.ang.brecho.application.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public interface SaleRepository {
  record Existing(UUID id, UUID actor, String hash) {}

  Optional<Existing> byRequestKey(UUID key);

  void create(
      UUID id, UUID customer, UUID actor, UUID key, String hash, BigDecimal total, String method);

  void item(UUID sale, UUID product, BigDecimal original, BigDecimal actual);

  Map<String, Object> detail(UUID id);

  List<Map<String, Object>> list(
      OffsetDateTime from, OffsetDateTime until, UUID customer, int page, int size);

  Map<String, Object> report(OffsetDateTime from, OffsetDateTime until);
}
