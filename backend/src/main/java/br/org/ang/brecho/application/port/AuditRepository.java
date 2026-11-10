package br.org.ang.brecho.application.port;

import java.util.*;

public interface AuditRepository {
  void record(UUID actor, String action, UUID entity, String detail);

  List<Map<String, Object>> list(int page, int size);
}
