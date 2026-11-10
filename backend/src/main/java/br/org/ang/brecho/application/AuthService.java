package br.org.ang.brecho.application;

import br.org.ang.brecho.application.port.*;
import br.org.ang.brecho.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  public record Session(String token, OffsetDateTime expiresAt, Actor usuario) {}

  private final IdentityRepository identities;
  private final LoginThrottle throttle;
  private final SessionIssuer sessions;
  private final PasswordEncoder passwords;
  private final String dummyHash;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      IdentityRepository identities,
      LoginThrottle throttle,
      PasswordEncoder passwords,
      SessionIssuer sessions) {
    this.identities = identities;
    this.throttle = throttle;
    this.passwords = passwords;
    this.sessions = sessions;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public Session login(Commands.Login command, String remoteAddress) {
    String username = command.usuario().trim().toLowerCase(Locale.ROOT);
    if (!throttle.allow(hash("ip:" + remoteAddress), 50)
        || !throttle.allow(hash("user:" + username), 10))
      throw new RuleViolation(429, "Muitas tentativas. Aguarde 15 minutos.");
    StaffAccount account = identities.byUsername(username).orElse(null);
    boolean valid =
        command.senha().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(
                command.senha(), account == null ? dummyHash : account.passwordHash());
    if (!valid || account == null || !account.active())
      throw new RuleViolation(401, "Usuário ou senha inválidos.");
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    OffsetDateTime expires = OffsetDateTime.now().plusHours(8);
    Actor current = sessions.issue(account, hash(token), expires);
    return new Session(token, expires, current);
  }

  public Optional<Actor> authenticate(String token) {
    return identities.byTokenHash(hash(token));
  }

  public void logout(String token) {
    identities.closeSession(hash(token));
  }

  public String encodePassword(String raw) {
    RuleViolation.require(
        raw != null && raw.length() >= 12 && raw.getBytes(StandardCharsets.UTF_8).length <= 72,
        "A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes.");
    return passwords.encode(raw);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
