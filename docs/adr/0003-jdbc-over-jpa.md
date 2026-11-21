# 3. JdbcClient instead of JPA
Status: accepted

**Context.** The services have small, explicit data models and rely on row locking (`FOR UPDATE`) and `ON CONFLICT`.
**Decision.** Use Spring's `JdbcClient` with SQL in the code. 
**Consequences.** Full control of locking and SQL, no lazy-loading surprises. More hand-written SQL; revisit if the models grow.
