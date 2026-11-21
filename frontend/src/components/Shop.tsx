import { money } from '../lib';
import { useCart } from '../stores/cart';
import { useProducts } from '../stores/products';

export default function Shop() {
  const { products, loading, error } = useProducts();
  const lines = useCart((s) => s.lines);
  const add = useCart((s) => s.add);

  return (
    <section>
      <h1>Gear for developers</h1>
      <p className="muted">Prices and stock come from the inventory service. Try a laptop (payment fails) or 4 laptop stands (out of stock).</p>
      {loading && products.length === 0 && <p className="muted">Loading products…</p>}
      {error && <p className="err">Could not load products: {error}. Is the inventory service running?</p>}
      <div className="grid">
        {products.map((p) => {
          const inCart = lines[p.id] ?? 0;
          return (
            <article key={p.id} className="tile">
              <div className="art" aria-hidden="true">{p.emoji}</div>
              <h3>{p.name}</h3>
              <div className="row">
                <strong>{money(p.price)}</strong>
                <span className={p.stock <= 3 ? 'low' : 'muted'}>{p.stock} in stock</span>
              </div>
              <button disabled={inCart >= Math.min(p.stock, 10)} onClick={() => add(p.id, p.stock)}>
                {inCart > 0 ? `Add another (${inCart} in cart)` : 'Add to cart'}
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}
