import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type CartState = {
  lines: Record<string, number>; // productId -> quantity
  /** Idempotency key for the current cart contents; renewed on every change so retries are safe but edits are not. */
  checkoutKey: string;
  add: (id: string, max: number) => void;
  dec: (id: string) => void;
  clear: () => void;
};

const newKey = () => crypto.randomUUID();

export const useCart = create<CartState>()(
  persist(
    (set) => ({
      lines: {},
      checkoutKey: newKey(),
      add: (id, max) => set((s) => ({
        lines: { ...s.lines, [id]: Math.min((s.lines[id] ?? 0) + 1, Math.min(max, 10)) },
        checkoutKey: newKey(),
      })),
      dec: (id) => set((s) => {
        const { [id]: q = 0, ...rest } = s.lines;
        return { lines: q > 1 ? { ...rest, [id]: q - 1 } : rest, checkoutKey: newKey() };
      }),
      clear: () => set({ lines: {}, checkoutKey: newKey() }),
    }),
    { name: 'shopstream-cart' },
  ),
);
