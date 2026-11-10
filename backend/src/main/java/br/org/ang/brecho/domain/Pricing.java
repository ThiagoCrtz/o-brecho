package br.org.ang.brecho.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class Pricing {
  private Pricing() {}

  public static BigDecimal money(BigDecimal value) {
    RuleViolation.require(
        value != null
            && value.signum() > 0
            && value.scale() <= 2
            && value.compareTo(new BigDecimal("9999999.99")) <= 0,
        "Informe um valor positivo com até duas casas decimais.");
    return value.setScale(2);
  }

  public static List<BigDecimal> distribute(List<BigDecimal> prices, BigDecimal total) {
    total = money(total);
    BigDecimal sum = prices.stream().map(Pricing::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    RuleViolation.require(
        total.compareTo(sum) <= 0, "O total não pode superar a soma dos preços negociados.");
    List<BigDecimal> values = new ArrayList<>();
    BigDecimal accumulated = BigDecimal.ZERO, allocated = BigDecimal.ZERO;
    for (BigDecimal price : prices) {
      accumulated = accumulated.add(price);
      BigDecimal target = total.multiply(accumulated).divide(sum, 2, RoundingMode.HALF_UP);
      values.add(target.subtract(allocated));
      allocated = target;
    }
    return List.copyOf(values);
  }
}
