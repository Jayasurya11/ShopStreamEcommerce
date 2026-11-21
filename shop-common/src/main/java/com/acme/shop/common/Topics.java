package com.acme.shop.common;

public final class Topics {
    private Topics() {}
    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String[] ALL = {ORDER_CREATED, INVENTORY_RESERVED, INVENTORY_REJECTED, PAYMENT_COMPLETED, PAYMENT_FAILED};
}
