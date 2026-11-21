# 1. Choreography-based saga for order placement
Status: accepted

**Context.** Placing an order touches three services that each own their data. Distributed transactions (2PC) are not available over Kafka and would couple the services.
**Decision.** Services react to each other's events (choreography). Failure is handled with compensating events (payment failed -> release stock).
**Consequences.** No central coordinator and loose coupling; the flow is harder to see in code, so it is documented in `architecture.md` and visible in traces. If the flow grows past ~5 steps, move to an orchestrator.
