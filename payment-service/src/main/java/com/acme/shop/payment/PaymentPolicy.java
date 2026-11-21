package com.acme.shop.payment;

import java.math.BigDecimal;
import java.util.Optional;

/** Mock payment gateway rule so the failure path is easy to demo. The limit comes from the config server. */
final class PaymentPolicy {
    private PaymentPolicy() {}

    static Optional<String> declineReason(BigDecimal amount, BigDecimal limit) {
        return amount.compareTo(limit) > 0
            ? Optional.of("Card declined: amounts above $" + limit.toPlainString() + " are not accepted in this demo")
            : Optional.empty();
    }
}
