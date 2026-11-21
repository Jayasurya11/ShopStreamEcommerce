# 2. Transactional outbox and idempotent consumers
Status: accepted

**Context.** "Save to the database, then publish to Kafka" can lose or duplicate events when the process fails in between.
**Decision.** Events are inserted in an outbox table in the same transaction as the state change and relayed to Kafka by a poller (`FOR UPDATE SKIP LOCKED`). Consumers record processed event ids and ignore duplicates.
**Consequences.** At-least-once delivery with effectively-once processing. Small publishing delay (poll interval 300 ms). Housekeeping removes old rows; `shop.outbox.pending` is monitored.
