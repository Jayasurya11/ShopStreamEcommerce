export type Product = { id: string; name: string; emoji: string; price: number; stock: number };
export type OrderStatus = 'PENDING' | 'AWAITING_PAYMENT' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
export type Order = {
  id: string; status: OrderStatus; total: number | null;
  customerEmail: string; createdAt: string; failureReason: string | null;
};
export type OrderUpdate = { orderId: string; status: OrderStatus; total: number | null; failureReason: string | null };
