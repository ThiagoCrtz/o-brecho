package br.org.ang.brecho.application.port;

import br.org.ang.brecho.application.Commands.*;
import br.org.ang.brecho.domain.Product;
import java.util.*;

public interface InventoryRepository {
  void receive(UUID donationId, UUID actor, Donation command);

  Product lock(UUID productId);

  boolean categoryExists(UUID categoryId);

  void approve(UUID id, Approve command);

  void changeStatus(UUID id, String status, String note);

  void move(UUID id, String location);

  List<Map<String, Object>> products(String status, int page, int size);

  List<Map<String, Object>> donations(int page, int size);
}
