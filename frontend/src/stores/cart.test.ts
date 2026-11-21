import { beforeEach, describe, expect, it } from 'vitest';
import { useCart } from './cart';

describe('cart store', () => {
  beforeEach(() => useCart.getState().clear());

  it('adds items and never exceeds the stock or 10 units', () => {
    const { add } = useCart.getState();
    for (let i = 0; i < 15; i++) add('p1', 4);
    expect(useCart.getState().lines.p1).toBe(4);
    for (let i = 0; i < 15; i++) add('p2', 50);
    expect(useCart.getState().lines.p2).toBe(10);
  });

  it('removes a line when its quantity reaches zero', () => {
    const { add, dec } = useCart.getState();
    add('p1', 5); add('p1', 5);
    dec('p1');
    expect(useCart.getState().lines.p1).toBe(1);
    dec('p1');
    expect(useCart.getState().lines.p1).toBeUndefined();
  });

  it('renews the idempotency key whenever the cart changes', () => {
    const before = useCart.getState().checkoutKey;
    useCart.getState().add('p1', 5);
    expect(useCart.getState().checkoutKey).not.toBe(before);
  });
});
