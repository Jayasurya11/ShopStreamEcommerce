import { useEffect } from 'react';
import Cart from './components/Cart';
import Orders from './components/Orders';
import Shop from './components/Shop';
import { useAuth } from './stores/auth';
import { useCart } from './stores/cart';
import { useOrders } from './stores/orders';
import { useProducts } from './stores/products';
import { useUi } from './stores/ui';

export default function App() {
  const view = useUi((s) => s.view);
  const setView = useUi((s) => s.setView);
  const toasts = useUi((s) => s.toasts);
  const live = useOrders((s) => s.live);
  const { name, email, logout } = useAuth();
  const connect = useOrders((s) => s.connect);
  const loadProducts = useProducts((s) => s.load);
  const cartCount = useCart((s) => Object.values(s.lines).reduce((a, b) => a + b, 0));

  useEffect(() => { void loadProducts(); return connect(); }, [loadProducts, connect]);

  return (
    <>
      <header className="bar">
        <strong className="logo">ShopStream</strong>
        <nav>
          <button aria-pressed={view === 'shop'} onClick={() => setView('shop')}>Shop{cartCount > 0 && <span className="pill">{cartCount}</span>}</button>
          <button aria-pressed={view === 'orders'} onClick={() => setView('orders')}>My orders</button>
        </nav>
        <span className={`live ${live ? 'on' : ''}`} title="Real-time connection to the order service">
          <i /> {live ? 'Live' : 'Reconnecting…'}
        </span>
        <span className="user" title={email}>{name || email}</span>
        <button className="ghost" onClick={logout}>Sign out</button>
      </header>

      {view === 'shop' ? (
        <main className="shop"><Shop /><Cart /></main>
      ) : (
        <main><Orders /></main>
      )}

      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => <div key={t.id} className={`toast ${t.tone}`}>{t.text}</div>)}
      </div>
    </>
  );
}
