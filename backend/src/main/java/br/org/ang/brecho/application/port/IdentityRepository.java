package br.org.ang.brecho.application.port;

import br.org.ang.brecho.application.Commands.Staff;
import br.org.ang.brecho.domain.*;
import java.time.OffsetDateTime;
import java.util.*;

public interface IdentityRepository {
  Optional<StaffAccount> byUsername(String username);

  Optional<Actor> byTokenHash(String hash);

  void create(UUID id, Staff data, String hash);

  StaffAccount lock(UUID id);

  void update(UUID id, Staff data);

  void password(UUID id, String hash);

  void revoke(UUID id);

  void openSession(String hash, UUID id, OffsetDateTime expires);

  void closeSession(String hash);

  List<Map<String, Object>> list(int page, int size);

  long count();

  void lockManagement();
}
