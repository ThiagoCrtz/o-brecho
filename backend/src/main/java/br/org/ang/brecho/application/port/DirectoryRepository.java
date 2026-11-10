package br.org.ang.brecho.application.port;

import br.org.ang.brecho.application.Commands.*;
import java.util.*;

public interface DirectoryRepository {
  void createCustomer(UUID id, Customer c);

  void updateCustomer(UUID id, Customer c);

  void archiveCustomer(UUID id);

  void lockActiveCustomer(UUID id);

  List<Map<String, Object>> customers(int page, int size);

  void createCategory(UUID id, Category c);

  List<Map<String, Object>> categories();
}
