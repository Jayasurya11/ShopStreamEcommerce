package com.acme.shop.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentPolicyTest {
    private static final BigDecimal LIMIT = new BigDecimal("1000.00");
    @Test void acceptsAmountAtTheLimit() {
        assertThat(PaymentPolicy.declineReason(new BigDecimal("1000.00"), LIMIT)).isEmpty();
    }
    @Test void declinesAmountAboveTheLimit() {
        assertThat(PaymentPolicy.declineReason(new BigDecimal("1000.01"), LIMIT)).isPresent();
    }
}
