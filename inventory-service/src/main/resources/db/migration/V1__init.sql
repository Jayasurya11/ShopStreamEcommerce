create table products (
  id uuid primary key default gen_random_uuid(),
  name varchar(120) not null,
  emoji varchar(8) not null,
  price numeric(12,2) not null check (price > 0),
  stock int not null check (stock >= 0)
);
create table reservations (
  order_id uuid not null,
  product_id uuid not null references products(id),
  quantity int not null,
  released boolean not null default false,
  primary key (order_id, product_id)
);
insert into products (name, emoji, price, stock) values
  ('Mechanical Keyboard', '⌨️', 129.00, 25),
  ('Wireless Mouse', '🖱️', 59.00, 40),
  ('4K Monitor', '🖥️', 449.00, 10),
  ('Noise-Cancelling Headphones', '🎧', 299.00, 15),
  ('HD Webcam', '📷', 89.00, 30),
  ('Aluminium Laptop Stand', '📐', 49.00, 3),
  ('Developer Laptop', '💻', 1499.00, 5);
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
