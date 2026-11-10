package br.org.ang.brecho.application;

import br.org.ang.brecho.application.Commands.*;
import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {
  private final SaleRepository sales;
  private final InventoryRepository inventory;
  private final DirectoryRepository directory;
  private final IdentityRepository identities;
  private final AuditRepository audit;
  private final ObjectMapper json;

  public SaleService(
      SaleRepository sales,
      InventoryRepository inventory,
      DirectoryRepository directory,
      IdentityRepository identities,
      AuditRepository audit,
      ObjectMapper json) {
    this.sales = sales;
    this.inventory = inventory;
    this.directory = directory;
    this.identities = identities;
    this.audit = audit;
    this.json = json;
  }

  @Transactional
  public Map<String, Object> sell(Actor actor, UUID requestKey, Sale command) {
    actor.requireSales();
    // Serializa reenvios do mesmo vendedor e verifica a conta antes de registrar a venda.
    StaffAccount current = identities.lock(actor.id());
    if (!current.active()) throw RuleViolation.forbidden();
    current.actor().requireSales();
    String hash = requestHash(command);
    var previous = sales.byRequestKey(requestKey);
    if (previous.isPresent()) {
      var found = previous.get();
      if (!found.actor().equals(actor.id()) || !found.hash().equals(hash))
        throw RuleViolation.conflict("Esta chave já foi utilizada em outra solicitação.");
      return sales.detail(found.id());
    }
    RuleViolation.require(
        (command.clienteId() != null) != (command.novoCliente() != null),
        "Informe um cliente existente ou os dados de um novo cliente.");
    var ordered = command.itens().stream().sorted(Comparator.comparing(Line::produtoId)).toList();
    RuleViolation.require(
        ordered.stream().map(Line::produtoId).distinct().count() == ordered.size(),
        "Há peças repetidas.");
    List<Product> products = new ArrayList<>();
    List<BigDecimal> prices = new ArrayList<>();
    for (Line line : ordered) {
      Product product = inventory.lock(line.produtoId());
      product.requireStatus("DISPONIVEL");
      BigDecimal price =
          line.precoNegociado() == null ? product.price() : Pricing.money(line.precoNegociado());
      RuleViolation.require(
          price.compareTo(product.price()) <= 0,
          "O preço negociado não pode superar o preço da peça.");
      products.add(product);
      prices.add(price);
    }
    BigDecimal total =
        command.total() == null
            ? prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            : command.total();
    List<BigDecimal> amounts = Pricing.distribute(prices, total);
    UUID customer = command.clienteId();
    if (customer == null) {
      customer = UUID.randomUUID();
      directory.createCustomer(customer, command.novoCliente());
      audit.record(actor.id(), "CLIENTE_CRIADO", customer, "Criado durante a venda.");
    } else directory.lockActiveCustomer(customer);
    UUID id = UUID.randomUUID();
    sales.create(id, customer, actor.id(), requestKey, hash, total, command.pagamento().name());
    for (int i = 0; i < products.size(); i++)
      sales.item(id, products.get(i).id(), products.get(i).price(), amounts.get(i));
    audit.record(actor.id(), "VENDA_CONCLUIDA", id, "Total: " + total.toPlainString());
    return sales.detail(id);
  }

  private String requestHash(Sale command) {
    try {
      return AuthService.hash(json.writeValueAsString(command));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Object> detail(UUID id) {
    return sales.detail(id);
  }

  public List<Map<String, Object>> list(
      LocalDate from, LocalDate to, UUID customer, int page, int size) {
    var period = period(from, to);
    return sales.list(period[0], period[1], customer, page, size);
  }

  public Map<String, Object> report(LocalDate from, LocalDate to) {
    var period = period(from, to);
    return sales.report(period[0], period[1]);
  }

  private OffsetDateTime[] period(LocalDate from, LocalDate to) {
    RuleViolation.require(
        !from.isAfter(to) && !to.isAfter(from.plusYears(10)),
        "Período inválido (máximo de 10 anos).");
    ZoneId zone = ZoneId.of("America/Sao_Paulo");
    return new OffsetDateTime[] {
      from.atStartOfDay(zone).toOffsetDateTime(),
      to.plusDays(1).atStartOfDay(zone).toOffsetDateTime()
    };
  }
}
