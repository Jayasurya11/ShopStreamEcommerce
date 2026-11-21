import { useEffect } from 'react';
import { money } from '../lib';
import { useOrders } from '../stores/orders';
import { useAuth } from '../stores/auth';
import type { Order } from '../types';

const STEPS = ['Order placed', 'Stock reserved', 'Payment', 'Confirmed'];

/** Maps the saga state to [steps completed, index of failed step or -1]. */
function progress(o: Order): { done: number; failed: number } {
  switch (o.status) {
    case 'PENDING': return { done: 1, failed: -1 };
    case 'AWAITING_PAYMENT': return { done: 2, failed: -1 };
    case 'CONFIRMED': return { done: 4, failed: -1 };
    case 'REJECTED': return { done: 1, failed: 1 };
    case 'CANCELLED': return { done: 2, failed: 2 };
  }
}

function Timeline({ order }: { order: Order }) {
  const { done, failed } = progress(order);
  return (
    <ol className="steps">
      {STEPS.map((label, i) => {
        const state = i === failed ? 'fail' : i < done ? 'done' : i === done && failed < 0 ? 'active' : '';
        return <li key={label} className={state}><span className="dot" />{label}</li>;
      })}
    </ol>
  );
}

export default function Orders() {
  const email = useAuth((s) => s.email);
  const orders = useOrders((s) => s.orders);
  const load = useOrders((s) => s.load);

  useEffect(() => { void load().catch(() => undefined); }, [load]);

  return (
    <section>
      <h1>Orders for {email}</h1>
      {orders.length === 0 && <p className="muted">No orders yet.</p>}
      {orders.map((o) => (
        <article key={o.id} className="order">
          <div className="row">
            <span className="muted">#{o.id.slice(0, 8)} · {new Date(o.createdAt).toLocaleString()}</span>
            <strong>{o.total != null ? money(o.total) : 'Pricing…'}</strong>
          </div>
          <Timeline order={o} />
          {o.failureReason && <p className="err">{o.failureReason}</p>}
        </article>
      ))}
    </section>
  );
}
