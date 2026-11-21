package com.acme.shop.payment;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mutable on purpose: POST /actuator/refresh re-binds these values without restarting the service. */
@ConfigurationProperties("shop.payment")
public class PaymentProperties {
    private long delayMs = 1200;
    private BigDecimal limit = new BigDecimal("1000.00");

    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
}
