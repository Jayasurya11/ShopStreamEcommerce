import { create } from 'zustand';

type Toast = { id: number; text: string; tone: 'ok' | 'bad' | 'info' };
type UiState = {
  view: 'shop' | 'orders';
  toasts: Toast[];
  setView: (v: 'shop' | 'orders') => void;
  notify: (text: string, tone?: Toast['tone']) => void;
};

let seq = 0;
export const useUi = create<UiState>((set) => ({
  view: 'shop',
  toasts: [],
  setView: (view) => set({ view }),
  notify: (text, tone = 'info') => {
    const id = ++seq;
    set((s) => ({ toasts: [...s.toasts, { id, text, tone }] }));
    setTimeout(() => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })), 4500);
  },
}));
