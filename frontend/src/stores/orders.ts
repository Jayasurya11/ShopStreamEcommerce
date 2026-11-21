import { fetchEventSource } from '@microsoft/fetch-event-source';
import { create } from 'zustand';
import { http } from '../lib';
import type { Order, OrderUpdate } from '../types';
import { useAuth } from './auth';
import { useProducts } from './products';
import { useUi } from './ui';

type OrdersState = {
  orders: Order[];
  live: boolean;
  load: () => Promise<void>;
  place: (items: { productId: string; quantity: number }[], idempotencyKey: string) => Promise<void>;
  applyUpdate: (u: OrderUpdate) => void;
  connect: () => () => void;
};

const MESSAGES: Record<string, [string, 'ok' | 'bad' | 'info']> = {
  AWAITING_PAYMENT: ['Stock reserved, charging your card…', 'info'],
  CONFIRMED: ['Order confirmed!', 'ok'],
  REJECTED: ['Order rejected: out of stock', 'bad'],
  CANCELLED: ['Payment failed, order cancelled and stock released', 'bad'],
};

export const useOrders = create<OrdersState>((set, get) => ({
  orders: [],
  live: false,

  load: async () => set({ orders: await http<Order[]>('/api/v1/orders') }),

  place: async (items, idempotencyKey) => {
    const order = await http<Order>('/api/v1/orders', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },   // a retry with the same key can never create a second order
      body: JSON.stringify({ items }),
    });
    set((s) => ({ orders: [order, ...s.orders] }));
  },

  applyUpdate: (u) => {
    if (!get().orders.some((o) => o.id === u.orderId)) return;
    set((s) => ({
      orders: s.orders.map((o) => (o.id === u.orderId ? { ...o, status: u.status, total: u.total, failureReason: u.failureReason } : o)),
    }));
    const msg = MESSAGES[u.status];
    if (msg) useUi.getState().notify(msg[0], msg[1]);
    void useProducts.getState().load(); // stock levels changed
  },

  // EventSource cannot send an Authorization header, so we use fetch-event-source (same protocol, custom headers).
  connect: () => {
    const abort = new AbortController();
    void fetchEventSource('/api/v1/orders/stream', {
      signal: abort.signal,
      openWhenHidden: true,
      fetch: async (input, init) => {
        const token = await useAuth.getState().token();
        return fetch(input, { ...init, headers: { ...init?.headers, Authorization: `Bearer ${token}` } });
      },
      async onopen(res) {
        if (!res.ok) throw new Error(`Live updates unavailable (${res.status})`);
        set({ live: true });
        void get().load().catch(() => undefined); // catch up on anything missed while disconnected
      },
      onmessage(ev) { if (ev.event === 'order') get().applyUpdate(JSON.parse(ev.data) as OrderUpdate); },
      onclose() { set({ live: false }); throw new Error('stream closed'); }, // throwing makes the library reconnect
      onerror() { set({ live: false }); },                                   // returning nothing = retry
    });
    return () => abort.abort();
  },
}));
