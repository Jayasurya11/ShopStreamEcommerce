package com.acme.shop.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    public record ProductDto(UUID id, String name, String emoji, BigDecimal price, int stock) {}
    private final JdbcClient jdbc;
    public ProductController(JdbcClient jdbc) { this.jdbc = jdbc; }

    @GetMapping("/api/v1/products")
    List<ProductDto> products() {
        return jdbc.sql("select id, name, emoji, price, stock from products order by price")
            .query((rs, n) -> new ProductDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("emoji"),
                rs.getBigDecimal("price"), rs.getInt("stock"))).list();
    }
}
