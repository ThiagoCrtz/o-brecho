package br.org.ang.brecho;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.org.ang.brecho.application.*;
import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import com.fasterxml.jackson.databind.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApiIntegrationTest {
  static final EmbeddedPostgres postgres = startPostgres();

  static EmbeddedPostgres startPostgres() {
    try {
      return EmbeddedPostgres.builder().setServerConfig("listen_addresses", "127.0.0.1").start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "postgres");
    r.add("app.bootstrap.username", () -> "");
    r.add("app.bootstrap.password", () -> "");
  }

  @AfterAll
  static void shutdown() throws Exception {
    postgres.close();
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;
  @Autowired IdentityRepository identities;
  @Autowired SaleService sales;
  UUID admin, attendant, triage, category, customer;
  static final String PASSWORD = "SenhaDeTeste123!";
  static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);

  @BeforeEach
  void setup() {
    db.execute(
        "TRUNCATE audit_event,payment,sale_item,sale,product,donation,category,customer,auth_session,staff,login_bucket CASCADE");
    admin = account("aline", Role.ADMINISTRADOR);
    attendant = account("joao", Role.ATENDENTE);
    triage = account("clara", Role.TRIAGEM);
    category = UUID.randomUUID();
    customer = UUID.randomUUID();
    db.update("INSERT INTO category VALUES (?, 'Roupas', '')", category);
    db.update("INSERT INTO customer VALUES (?, 'Cliente', '11999999999', '', '', TRUE)", customer);
  }

  UUID account(String username, Role role) {
    UUID id = UUID.randomUUID();
    identities.create(id, new Commands.Staff(username, username, role, "", true), HASH);
    identities.openSession(AuthService.hash(id.toString()), id, OffsetDateTime.now().plusHours(1));
    return id;
  }

  ResultActions request(MockHttpServletRequestBuilder request, UUID user, Object body)
      throws Exception {
    request.header("Authorization", "Bearer " + user);
    if (body != null) request.contentType("application/json").content(json.writeValueAsBytes(body));
    return mvc.perform(request);
  }

  UUID piece(String status) {
    UUID donation = UUID.randomUUID(), id = UUID.randomUUID();
    db.update(
        "INSERT INTO donation VALUES (?,'Doador','',?,?)", donation, OffsetDateTime.now(), admin);
    db.update(
        """
            INSERT INTO product(id,donation_id,name,description,size,brand,condition,category_id,price,status,location,notes)
            VALUES (?,?,'Camisa','','M','Marca','BOM',?,100,?,'Prateleira','')
            """,
        id,
        donation,
        category,
        status);
    return id;
  }

  Map<String, Object> approval() {
    return Map.of(
        "categoriaId", category, "preco", 100, "localizacao", "Arara", "observacao", "Revisada");
  }

  Commands.Sale sale(UUID product, BigDecimal total) {
    return new Commands.Sale(
        customer,
        null,
        List.of(new Commands.Line(product, null)),
        total,
        Commands.PaymentMethod.PIX);
  }

  @Test
  void allPrivateRoutesRequireLogin() throws Exception {
    mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/health")).andExpect(status().isOk());
    request(get("/api/produtos"), UUID.randomUUID(), null).andExpect(status().isUnauthorized());
  }

  @Test
  void claraCanApproveButAttendantCannot() throws Exception {
    UUID id = piece("PENDENTE");
    request(post("/api/triagens/" + id + "/aprovar"), attendant, approval())
        .andExpect(status().isForbidden());
    request(post("/api/triagens/" + id + "/aprovar"), triage, approval())
        .andExpect(status().isNoContent());
    request(post("/api/triagens/" + id + "/aprovar"), admin, approval())
        .andExpect(status().isConflict());
    assertEquals(
        "DISPONIVEL", db.queryForObject("SELECT status FROM product WHERE id=?", String.class, id));
    assertEquals(
        triage,
        db.queryForObject("SELECT actor_id FROM audit_event WHERE entity_id=?", UUID.class, id));
  }

  @Test
  void donationCreatesOnlyPendingProductsAndRejectsForgedActor() throws Exception {
    var body =
        Map.of(
            "origem",
            "Condomínio",
            "observacao",
            "Recebida",
            "pecas",
            List.of(
                Map.of(
                    "nome",
                    "Camisa",
                    "descricao",
                    "",
                    "tamanho",
                    "M",
                    "marca",
                    "Sem marca",
                    "estado",
                    "BOM")));
    request(post("/api/doacoes"), triage, body).andExpect(status().isCreated());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM product WHERE status='PENDENTE' AND price=0", Integer.class));
    assertEquals(triage, db.queryForObject("SELECT received_by FROM donation", UUID.class));
    var forged = new HashMap<String, Object>(body);
    forged.put("funcionarioId", admin);
    request(post("/api/doacoes"), triage, forged).andExpect(status().isBadRequest());
    assertEquals(1, db.queryForObject("SELECT count(*) FROM donation", Integer.class));
  }

  @Test
  void rejectingAndReopeningRespectState() throws Exception {
    UUID id = piece("PENDENTE");
    request(post("/api/triagens/" + id + "/descartar"), attendant, Map.of("observacao", "Rasgada"))
        .andExpect(status().isNoContent());
    request(post("/api/triagens/" + id + "/reavaliar"), triage, Map.of("observacao", "Reparada"))
        .andExpect(status().isNoContent());
    request(post("/api/triagens/" + id + "/reavaliar"), triage, Map.of("observacao", "Repetida"))
        .andExpect(status().isConflict());
  }

  @Test
  void attendantCanDiscountAndRetryWithoutDuplicating() throws Exception {
    UUID id = piece("DISPONIVEL"), key = UUID.randomUUID();
    var body = sale(id, new BigDecimal("59.90"));
    var first =
        request(post("/api/vendas").header("Idempotency-Key", key), attendant, body)
            .andExpect(status().isCreated())
            .andReturn();
    var second =
        request(post("/api/vendas").header("Idempotency-Key", key), attendant, body)
            .andExpect(status().isCreated())
            .andReturn();
    assertEquals(
        json.readTree(first.getResponse().getContentAsString()).get("id"),
        json.readTree(second.getResponse().getContentAsString()).get("id"));
    assertEquals(
        new BigDecimal("59.90"),
        db.queryForObject("SELECT sum(unit_price) FROM sale_item", BigDecimal.class));
    assertEquals(
        new BigDecimal("59.90"), db.queryForObject("SELECT amount FROM payment", BigDecimal.class));
    assertEquals(1, db.queryForObject("SELECT count(*) FROM sale", Integer.class));
    request(
            post("/api/vendas").header("Idempotency-Key", key),
            attendant,
            sale(id, new BigDecimal("50.00")))
        .andExpect(status().isConflict());
    request(post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()), attendant, body)
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsSaleByTriageAndUnapprovedProduct() throws Exception {
    UUID id = piece("PENDENTE");
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            triage,
            sale(id, null))
        .andExpect(status().isForbidden());
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            sale(id, null))
        .andExpect(status().isConflict());
    assertEquals(0, db.queryForObject("SELECT count(*) FROM payment", Integer.class));
  }

  @Test
  void duplicateItemsBadPriceAndUnknownPaymentDoNotChangeStock() throws Exception {
    UUID id = piece("DISPONIVEL");
    var duplicate =
        new Commands.Sale(
            customer,
            null,
            List.of(new Commands.Line(id, null), new Commands.Line(id, null)),
            null,
            Commands.PaymentMethod.PIX);
    request(post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()), attendant, duplicate)
        .andExpect(status().isUnprocessableEntity());
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            sale(id, new BigDecimal("100.01")))
        .andExpect(status().isUnprocessableEntity());
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            Map.of(
                "clienteId",
                customer,
                "itens",
                List.of(Map.of("produtoId", id)),
                "pagamento",
                "CHEQUE"))
        .andExpect(status().isBadRequest());
    assertEquals(
        "DISPONIVEL", db.queryForObject("SELECT status FROM product WHERE id=?", String.class, id));
  }

  @Test
  void invalidCustomerRollsBackSale() throws Exception {
    UUID id = piece("DISPONIVEL");
    var body =
        new Commands.Sale(
            UUID.randomUUID(),
            null,
            List.of(new Commands.Line(id, null)),
            null,
            Commands.PaymentMethod.PIX);
    request(post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()), attendant, body)
        .andExpect(status().isNotFound());
    assertEquals(0, db.queryForObject("SELECT count(*) FROM sale", Integer.class));
    assertEquals(
        "DISPONIVEL", db.queryForObject("SELECT status FROM product WHERE id=?", String.class, id));
  }

  @Test
  void paymentFailureRollsBackNewCustomerStockAndSale() throws Exception {
    UUID id = piece("DISPONIVEL");
    db.execute("ALTER TABLE payment ADD CONSTRAINT test_failure CHECK (amount<1)");
    try {
      var body =
          new Commands.Sale(
              null,
              new Commands.Customer("Nova", "11999999999", "", ""),
              List.of(new Commands.Line(id, null)),
              null,
              Commands.PaymentMethod.PIX);
      request(post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()), attendant, body)
          .andExpect(status().isConflict());
      assertEquals(1, db.queryForObject("SELECT count(*) FROM customer", Integer.class));
      assertEquals(0, db.queryForObject("SELECT count(*) FROM sale", Integer.class));
      assertEquals(
          "DISPONIVEL",
          db.queryForObject("SELECT status FROM product WHERE id=?", String.class, id));
    } finally {
      db.execute("ALTER TABLE payment DROP CONSTRAINT test_failure");
    }
  }

  @Test
  void concurrentSalesHaveExactlyOneWinner() throws Exception {
    UUID id = piece("DISPONIVEL");
    var executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
    try {
      List<Future<Boolean>> outcomes = new ArrayList<>();
      for (Actor actor :
          List.of(
              new Actor(admin, "Aline", Role.ADMINISTRADOR),
              new Actor(attendant, "João", Role.ATENDENTE))) {
        outcomes.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  go.await();
                  try {
                    sales.sell(actor, UUID.randomUUID(), sale(id, null));
                    return true;
                  } catch (RuleViolation e) {
                    assertEquals(409, e.status());
                    return false;
                  }
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      go.countDown();
      int wins = 0;
      for (var result : outcomes) if (result.get(15, TimeUnit.SECONDS)) wins++;
      assertEquals(1, wins);
      assertEquals(1, db.queryForObject("SELECT count(*) FROM sale_item", Integer.class));
      assertEquals(1, db.queryForObject("SELECT count(*) FROM payment", Integer.class));
    } finally {
      go.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void wholeTeamCanReadFinanceAndHistoryButNotManageUsers() throws Exception {
    for (UUID user : List.of(admin, attendant, triage)) {
      request(get("/api/relatorios?inicio=2026-01-01&fim=2026-12-31"), user, null)
          .andExpect(status().isOk());
      request(get("/api/vendas?inicio=2026-01-01&fim=2026-12-31&clienteId=" + customer), user, null)
          .andExpect(status().isOk());
    }
    request(get("/api/equipe"), triage, null).andExpect(status().isForbidden());
    request(get("/api/equipe"), attendant, null).andExpect(status().isForbidden());
    String staff =
        request(get("/api/equipe"), admin, null)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(staff.contains("password"));
    assertFalse(staff.contains(HASH));
  }

  @Test
  void userChangesRevokeSessionsAndCannotRemoveOwnManagerRole() throws Exception {
    request(
            put("/api/equipe/" + triage),
            admin,
            new Commands.Staff("Clara", "clara", Role.TRIAGEM, "", false))
        .andExpect(status().isNoContent());
    request(get("/api/produtos"), triage, null).andExpect(status().isUnauthorized());
    request(
            put("/api/equipe/" + admin),
            admin,
            new Commands.Staff("Aline", "aline", Role.ATENDENTE, "", true))
        .andExpect(status().isUnprocessableEntity());
    request(get("/api/equipe"), admin, null).andExpect(status().isOk());
  }

  @Test
  void loginLogoutAndExpiredSessions() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(json.writeValueAsBytes(new Commands.Login("aline", "errada"))))
        .andExpect(status().isUnauthorized());
    var response =
        mvc.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(json.writeValueAsBytes(new Commands.Login("aline", PASSWORD))))
            .andExpect(status().isOk())
            .andReturn();
    String token = json.readTree(response.getResponse().getContentAsString()).get("token").asText();
    assertEquals(43, token.length());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM auth_session WHERE token_hash=?",
            Integer.class,
            AuthService.hash(token)));
    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    db.update(
        "UPDATE auth_session SET expires_at=? WHERE staff_id=?",
        OffsetDateTime.now().minusHours(1),
        admin);
    request(get("/api/produtos"), admin, null).andExpect(status().isUnauthorized());
  }

  @Test
  void repeatedFailedLoginIsThrottled() throws Exception {
    for (int i = 0; i < 10; i++)
      mvc.perform(
              post("/api/auth/login")
                  .contentType("application/json")
                  .content("{\"usuario\":\"naoexiste\",\"senha\":\"invalida\"}"))
          .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"usuario\":\"naoexiste\",\"senha\":\"invalida\"}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void invalidPaginationDatesAndAmountsAreRejected() throws Exception {
    request(get("/api/produtos?tamanho=1000"), admin, null).andExpect(status().isBadRequest());
    request(get("/api/relatorios?inicio=2026-12-31&fim=2026-01-01"), admin, null)
        .andExpect(status().isUnprocessableEntity());
    UUID id = piece("PENDENTE");
    var body = new HashMap<>(approval());
    body.put("preco", -1);
    request(post("/api/triagens/" + id + "/aprovar"), triage, body)
        .andExpect(status().isBadRequest());
  }

  @Test
  void archivedCustomerKeepsHistoryAndCannotMakeNewPurchase() throws Exception {
    UUID sold = piece("DISPONIVEL"), available = piece("DISPONIVEL");
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            sale(sold, null))
        .andExpect(status().isCreated());
    request(post("/api/clientes/" + customer + "/arquivar"), admin, null)
        .andExpect(status().isNoContent());
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            sale(available, null))
        .andExpect(status().isUnprocessableEntity());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM sale WHERE customer_id=?", Integer.class, customer));
  }

  @Test
  void passwordResetRevokesAccessAndOldPassword() throws Exception {
    request(
            post("/api/equipe/" + attendant + "/senha"),
            admin,
            new Commands.Password("NovaSenhaSegura123!"))
        .andExpect(status().isNoContent());
    request(get("/api/auth/me"), attendant, null).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(json.writeValueAsBytes(new Commands.Login("joao", PASSWORD))))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(json.writeValueAsBytes(new Commands.Login("joao", "NovaSenhaSegura123!"))))
        .andExpect(status().isOk());
  }

  @Autowired SessionIssuer sessionIssuer;

  @Test
  void loginCannotIssueSessionAfterPasswordChanged() {
    var previouslyVerified = identities.byUsername("joao").orElseThrow();
    identities.password(attendant, new BCryptPasswordEncoder(4).encode("OutraSenhaSegura123!"));
    var error =
        assertThrows(
            RuleViolation.class,
            () ->
                sessionIssuer.issue(
                    previouslyVerified,
                    AuthService.hash("novo-token"),
                    OffsetDateTime.now().plusHours(1)));
    assertEquals(401, error.status());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM auth_session WHERE token_hash=?",
            Integer.class,
            AuthService.hash("novo-token")));
  }

  @Test
  void managerCreatesAccountsAndSensitiveFieldsAreNeverReturned() throws Exception {
    var command =
        new Commands.CreateStaff(
            new Commands.Staff("Maria", "maria", Role.TRIAGEM, "", true), PASSWORD);
    request(post("/api/equipe"), attendant, command).andExpect(status().isForbidden());
    request(post("/api/equipe"), admin, command).andExpect(status().isCreated());
    request(post("/api/equipe"), admin, command).andExpect(status().isConflict());
    String hash =
        db.queryForObject("SELECT password_hash FROM staff WHERE username='maria'", String.class);
    assertNotEquals(PASSWORD, hash);
    assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, hash));
  }

  @Test
  void reportMatchesDiscountedSaleAndPayment() throws Exception {
    UUID id = piece("DISPONIVEL");
    request(
            post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()),
            attendant,
            sale(id, new BigDecimal("59.90")))
        .andExpect(status().isCreated());
    LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    request(get("/api/relatorios?inicio=" + day + "&fim=" + day), triage, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vendas").value(1))
        .andExpect(jsonPath("$.recebido").value(59.90))
        .andExpect(jsonPath("$.pecas").value(1))
        .andExpect(jsonPath("$.porPagamento[0].recebido").value(59.90))
        .andExpect(jsonPath("$.porCategoria[0].recebido").value(59.90));
  }

  @Test
  void validNewCustomerIsCreatedAtomicallyWithSale() throws Exception {
    UUID id = piece("DISPONIVEL");
    var body =
        new Commands.Sale(
            null,
            new Commands.Customer("Nova", "11999999999", "", ""),
            List.of(new Commands.Line(id, null)),
            null,
            Commands.PaymentMethod.DINHEIRO);
    request(post("/api/vendas").header("Idempotency-Key", UUID.randomUUID()), attendant, body)
        .andExpect(status().isCreated());
    assertEquals(2, db.queryForObject("SELECT count(*) FROM customer", Integer.class));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM sale s JOIN customer c ON c.id=s.customer_id WHERE c.name='Nova'",
            Integer.class));
  }

  @Test
  void requestSizeAndForeignOriginsAreRejected() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json").content("x".repeat(65537)))
        .andExpect(status().isPayloadTooLarge());
    mvc.perform(
            options("/api/produtos")
                .header("Origin", "https://outro-site.invalid")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
    request(delete("/api/clientes/" + customer), admin, null)
        .andExpect(status().isMethodNotAllowed());
  }

  @org.springframework.boot.test.web.server.LocalServerPort int serverPort;

  @Test
  void actualHttpServerAcceptsLoginAndAuthenticatedRequest() throws Exception {
    var client = java.net.http.HttpClient.newHttpClient();
    String body = json.writeValueAsString(new Commands.Login("aline", PASSWORD));
    var response =
        client.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://127.0.0.1:" + serverPort + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    String token = json.readTree(response.body()).get("token").asText();
    var me =
        client.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://127.0.0.1:" + serverPort + "/api/auth/me"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(200, me.statusCode());
    assertEquals("ADMINISTRADOR", json.readTree(me.body()).get("role").asText());
  }
}
