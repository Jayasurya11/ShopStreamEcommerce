# Security policy
Report vulnerabilities privately to the maintainers (do not open a public issue).

Controls in this repository: OIDC with PKCE, JWT validation at the gateway, rate limiting, security headers, non-root and scanned container images (Trivy), CodeQL, Dependabot, no secrets in git (`.env` is ignored; use a secret manager in production).
Known demo-only settings: Keycloak `start-dev`, default passwords in `.env.example`, a mock payment gateway.
