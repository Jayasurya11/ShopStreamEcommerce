# 5. Central configuration without secrets
Status: accepted

**Context.** Ports, hosts and tuning must be consistent and changeable without rebuilding images.
**Decision.** Spring Cloud Config serves non-secret settings from `config-repo` (Git in production). Secrets (DB credentials, config-server password) come from environment variables / Kubernetes Secrets.
**Consequences.** Live changes via `/actuator/refresh` for `@ConfigurationProperties`. Lists in profile files replace base lists, so gateway routes are duplicated per profile.
