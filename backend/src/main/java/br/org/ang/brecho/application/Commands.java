package br.org.ang.brecho.application;

import br.org.ang.brecho.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class Commands {
  private Commands() {}

  public record Login(
      @NotBlank @Size(max = 60) String usuario, @NotBlank @Size(max = 72) String senha) {}

  public record Staff(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,60}") String usuario,
      @NotNull Role perfil,
      @NotNull @Size(max = 25) String telefone,
      @NotNull Boolean ativo) {}

  public record CreateStaff(
      @NotNull @Valid Staff dados, @NotBlank @Size(min = 12, max = 72) String senha) {}

  public record Password(@NotBlank @Size(min = 12, max = 72) String senha) {}

  public record Customer(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Pattern(regexp = "[+()0-9 .-]{8,25}") String telefone,
      @NotNull @Email @Size(max = 254) String email,
      @NotNull @Size(max = 1000) String observacoes) {}

  public record Category(
      @NotBlank @Size(max = 120) String nome, @NotNull @Size(max = 500) String descricao) {}

  public record Piece(
      @NotBlank @Size(max = 120) String nome,
      @NotNull @Size(max = 1000) String descricao,
      @NotBlank @Size(max = 30) String tamanho,
      @NotBlank @Size(max = 80) String marca,
      @NotNull Condition estado) {}

  public enum Condition {
    NOVO,
    BOM,
    REGULAR,
    PARA_REPARO
  }

  public record Donation(
      @NotBlank @Size(max = 160) String origem,
      @NotNull @Size(max = 1000) String observacao,
      @NotEmpty @Size(max = 100) List<@NotNull @Valid Piece> pecas) {}

  public record Approve(
      @NotNull UUID categoriaId,
      @NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2)
          BigDecimal preco,
      @NotBlank @Size(max = 120) String localizacao,
      @NotNull @Size(max = 1000) String observacao) {}

  public record Note(@NotBlank @Size(max = 1000) String observacao) {}

  public record Stock(@NotBlank @Size(max = 120) String localizacao) {}

  public record Line(
      @NotNull UUID produtoId,
      @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2)
          BigDecimal precoNegociado) {}

  public enum PaymentMethod {
    PIX,
    DINHEIRO,
    CARTAO
  }

  public record Sale(
      UUID clienteId,
      @Valid Customer novoCliente,
      @NotEmpty @Size(max = 100) List<@NotNull @Valid Line> itens,
      @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2)
          BigDecimal total,
      @NotNull PaymentMethod pagamento) {}
}
