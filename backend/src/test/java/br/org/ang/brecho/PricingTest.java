package br.org.ang.brecho;

import static org.junit.jupiter.api.Assertions.*;

import br.org.ang.brecho.domain.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {
  @Test
  void discountPreservesEveryCent() {
    for (int cents = 1; cents < 10000; cents += 13) {
      var total = BigDecimal.valueOf(cents, 2);
      var values =
          Pricing.distribute(
              List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")),
              total);
      assertEquals(total, values.stream().reduce(new BigDecimal("0.00"), BigDecimal::add));
      assertTrue(values.stream().allMatch(v -> v.signum() >= 0));
    }
  }

  @Test
  void rejectsNegativeZeroExcessAndFractionalCents() {
    for (String value : List.of("-1", "0", "100.001", "101"))
      assertThrows(
          RuleViolation.class,
          () -> Pricing.distribute(List.of(new BigDecimal("100.00")), new BigDecimal(value)));
  }
}
