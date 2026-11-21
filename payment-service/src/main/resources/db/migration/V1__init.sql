create table payments (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null unique,
  amount numeric(12,2) not null,
  status varchar(20) not null,
  reason varchar(200),
  created_at timestamptz not null default now()
);
create table outbox_events (
  id bigserial primary key,
  topic varchar(100) not null,
  msg_key varchar(100) not null,
  payload text not null,
  created_at timestamptz not null default now(),
  sent_at timestamptz
);
create index outbox_unsent on outbox_events (id) where sent_at is null;
create table processed_events (
  event_id uuid primary key,
  processed_at timestamptz not null default now()
);
