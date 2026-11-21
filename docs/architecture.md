# Architecture

## Containers
```mermaid
flowchart LR
  U[Browser: React + Zustand] -->|OIDC + PKCE| KC[(Keycloak)]
  U -->|Bearer JWT| GW[api-gateway]
  GW -->|rate limit| R[(Redis)]
  GW --> O[order-service] & I[inventory-service]
  O & I & P[payment-service] <-->|events| K{{Kafka}}
  O --> ODB[(orders db)]
  I --> IDB[(inventory db)]
  P --> PDB[(payments db)]
  CS[config-server] -.-> GW & O & I & P
  GW & O & I & P -. traces .-> J[Jaeger]
  GW & O & I & P -. metrics .-> PR[Prometheus] --> GF[Grafana]
```

## Place-order saga (choreography)
```mermaid
sequenceDiagram
  participant UI
  participant GW as Gateway
  participant O as Order
  participant I as Inventory
  participant P as Payment
  UI->>GW: POST /api/v1/orders (JWT, Idempotency-Key)
  GW->>O: X-User-Email (from validated token)
  O->>O: save order PENDING + outbox row (one transaction)
  O-->>I: order.created
  I->>I: lock rows, reserve stock, price the order
  alt not enough stock
    I-->>O: inventory.rejected  → REJECTED
  else reserved
    I-->>O: inventory.reserved → AWAITING_PAYMENT
    I-->>P: inventory.reserved
    alt payment ok
      P-->>O: payment.completed → CONFIRMED
    else declined
      P-->>O: payment.failed → CANCELLED
      P-->>I: payment.failed → release stock (compensation)
    end
  end
  O-->>UI: live status over SSE after each commit
```

## Data ownership
| Service | Owns | Publishes | Consumes |
|---|---|---|---|
| order-service | orders, order_items, idempotency_keys | order.created | inventory.reserved/rejected, payment.completed/failed |
| inventory-service | products, reservations | inventory.reserved/rejected | order.created, payment.failed |
| payment-service | payments | payment.completed/failed | inventory.reserved |

No service reads another service's database. Prices are decided server-side by inventory-service.

## Quality attributes
| Attribute | How it is addressed |
|---|---|
| Consistency | Saga with compensation; transactional outbox; idempotent consumers; idempotent producer |
| Availability | Stateless services, graceful shutdown, readiness/liveness probes, retries with back-off, dead-letter topics, circuit breakers |
| Security | OIDC/PKCE, JWT validated at the gateway, identity headers rebuilt from the token, network policies, no secrets in git, non-root containers, scanned images |
| Observability | Prometheus metrics + alerts, Grafana dashboard, distributed traces across HTTP and Kafka, structured JSON logs in containers |
| Operability | Central config with live refresh, Flyway migrations, Kubernetes manifests with HPA and PDB |
| Testability | Unit tests, Testcontainers integration test (real Kafka + PostgreSQL), frontend store tests, CI quality gates |
