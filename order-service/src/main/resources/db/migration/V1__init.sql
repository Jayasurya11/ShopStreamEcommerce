create table orders (
  id uuid primary key,
  customer_email varchar(200) not null,
  status varchar(20) not null,
  total numeric(12,2),
  failure_reason varchar(200),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index orders_email on orders (customer_email, created_at desc);
create table order_items (
  order_id uuid not null references orders(id),
  product_id uuid not null,
  quantity int not null check (quantity > 0),
  primary key (order_id, product_id)
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
