import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./auth', () => ({ useAuth: { getState: () => ({ token: async () => 'test-token' }) } }));
vi.mock('./products', () => ({ useProducts: { getState: () => ({ load: vi.fn() }) } }));

import type { Order } from '../types';
import { useOrders } from './orders';

const order = (id: string): Order => ({
  id, status: 'PENDING', total: null, customerEmail: 'a@b.c', createdAt: '2026-01-01T00:00:00Z', failureReason: null,
});

describe('orders store', () => {
  beforeEach(() => useOrders.setState({ orders: [order('o1')] }));

  it('applies a live update to a known order', () => {
    useOrders.getState().applyUpdate({ orderId: 'o1', status: 'CONFIRMED', total: 129, failureReason: null });
    expect(useOrders.getState().orders[0]).toMatchObject({ status: 'CONFIRMED', total: 129 });
  });

  it('ignores updates for orders it does not know', () => {
    useOrders.getState().applyUpdate({ orderId: 'other', status: 'CANCELLED', total: 1, failureReason: 'x' });
    expect(useOrders.getState().orders).toHaveLength(1);
    expect(useOrders.getState().orders[0].status).toBe('PENDING');
  });
});
