import { create } from 'zustand';
import { http } from '../lib';
import type { Product } from '../types';

type ProductsState = { products: Product[]; loading: boolean; error?: string; load: () => Promise<void> };

export const useProducts = create<ProductsState>((set) => ({
  products: [],
  loading: false,
  load: async () => {
    set({ loading: true, error: undefined });
    try { set({ products: await http<Product[]>('/api/v1/products'), loading: false }); }
    catch (e) { set({ loading: false, error: (e as Error).message }); }
  },
}));
