import { useAuth } from './stores/auth';

export const money = (n: number) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

/** Every API call goes through the gateway with the user's Keycloak access token. */
export async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const token = await useAuth.getState().token();
  const res = await fetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, ...init?.headers },
  });
  if (res.status === 429) throw new Error('You are doing that too quickly. Please wait a moment and try again.');
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.detail ?? body.message ?? `Request failed (${res.status})`);
  }
  return res.json();
}
