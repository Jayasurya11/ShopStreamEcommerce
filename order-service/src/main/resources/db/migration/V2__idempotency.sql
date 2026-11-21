create table idempotency_keys (
  customer_email varchar(200) not null,
  idem_key varchar(80) not null,
  order_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (customer_email, idem_key)
);
