package br.org.ang.brecho.web;

import br.org.ang.brecho.application.*;
import br.org.ang.brecho.domain.Actor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/api/auth/login")
  public AuthService.Session login(
      @Valid @RequestBody Commands.Login c, HttpServletRequest request) {
    return auth.login(c, request.getRemoteAddr());
  }

  @GetMapping("/api/auth/me")
  public Actor me(@AuthenticationPrincipal Actor actor) {
    return actor;
  }

  @PostMapping("/api/auth/logout")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader("Authorization") String authorization) {
    auth.logout(authorization.substring(7));
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }
}
