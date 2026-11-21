# Runbook

| Symptom / alert | Likely cause | What to do |
|---|---|---|
| `OutboxBacklog` | Kafka unreachable or broker down | Check Kafka health; the relay retries automatically and keeps event order. Backlog drains when Kafka is back. |
| `KafkaConsumerLag` | Slow consumer or crashed pod | Check pod restarts and the p95 panel; scale the deployment. |
| Messages in `<topic>.DLT` | A message failed 5 times (poison message) | Inspect the payload in Kafka UI, fix the cause (bug / data), then re-publish the message to the original topic. Handlers are idempotent, so replay is safe. |
| Orders stuck in `PENDING` | inventory-service down or lagging | Check `order.created` consumer lag; once the service is back the order continues. |
| Orders stuck in `AWAITING_PAYMENT` | payment-service down | Same; payments resume from `inventory.reserved`. |
| Users get 429 | Rate limit (5 req/s per user for orders) | Expected; check for a misbehaving client. Limits are in `config-repo/api-gateway.yml`. |
| Users get 503 "service unavailable" | Circuit breaker open | Check the downstream service; the circuit probes again after 10 s. |
| 401 everywhere | Keycloak down or issuer mismatch | Check Keycloak; the token `iss` must equal `issuer-uri` in the gateway config. |

**Traces:** open Jaeger (`:16686`), search service `api-gateway`, pick a trace to see gateway -> order -> Kafka -> inventory -> payment.
**Rolling back:** `kubectl rollout undo deployment/<name>`. Database migrations are forward-only; make them backward compatible (expand, then contract).
**Rotating secrets:** update the Kubernetes Secret and restart the deployments (`kubectl rollout restart`).
