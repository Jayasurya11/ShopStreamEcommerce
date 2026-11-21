import { useState } from 'react';
import { money } from '../lib';
import { useCart } from '../stores/cart';
import { useOrders } from '../stores/orders';
import { useProducts } from '../stores/products';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';

export default function Cart() {
  const { lines, checkoutKey, add, dec, clear } = useCart();
  const products = useProducts((s) => s.products);
  const email = useAuth((s) => s.email);
  const place = useOrders((s) => s.place);
  const setView = useUi((s) => s.setView);
  const notify = useUi((s) => s.notify);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const items = Object.entries(lines).flatMap(([id, qty]) => {
    const p = products.find((x) => x.id === id);
    return p ? [{ p, qty }] : [];
  });
  const total = items.reduce((sum, { p, qty }) => sum + p.price * qty, 0);

  async function checkout() {
    setBusy(true); setError('');
    try {
      await place(items.map(({ p, qty }) => ({ productId: p.id, quantity: qty })), checkoutKey);
      clear();
      notify('Order placed. Watch it progress in real time.', 'info');
      setView('orders');
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }

  return (
    <aside className="cart" aria-label="Shopping cart">
      <h2>Your cart</h2>
      {items.length === 0 && <p className="muted">Your cart is empty.</p>}
      <ul>
        {items.map(({ p, qty }) => (
          <li key={p.id}>
            <span>{p.emoji} {p.name}</span>
            <span className="qty">
              <button className="ghost" aria-label={`Remove one ${p.name}`} onClick={() => dec(p.id)}>−</button>
              {qty}
              <button className="ghost" aria-label={`Add one ${p.name}`} onClick={() => add(p.id, p.stock)}>+</button>
            </span>
          </li>
        ))}
      </ul>
      {items.length > 0 && (
        <>
          <div className="row total"><span>Total</span><strong>{money(total)}</strong></div>
          <p className="muted">Ordering as {email}</p>
          {error && <p className="err">{error}</p>}
          <button disabled={busy} onClick={checkout}>{busy ? 'Placing order…' : 'Place order'}</button>
        </>
      )}
    </aside>
  );
}
