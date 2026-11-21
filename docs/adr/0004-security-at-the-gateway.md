# 4. Authenticate at the gateway, propagate identity by header
Status: accepted

**Context.** Every service validating JWTs duplicates configuration and couples services to the identity provider.
**Decision.** Keycloak authenticates users. The gateway validates JWTs and sets `X-User-Id` / `X-User-Email` after stripping client-supplied values. Services trust those headers; Kubernetes NetworkPolicies allow only the gateway to reach them.
**Consequences.** Simple services. Anything that can reach a service port can forge identity, so network isolation is mandatory (docker compose publishes only the gateway). Move to mTLS / a service mesh or per-service JWT validation for stricter zero-trust.
