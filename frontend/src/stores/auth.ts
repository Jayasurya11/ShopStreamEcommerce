import Keycloak from 'keycloak-js';
import { create } from 'zustand';
import { useCart } from './cart';

type Claims = { email?: string; name?: string; preferred_username?: string; realm_access?: { roles?: string[] } };

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'shopstream',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'shopstream-web',
});

type AuthState = {
  email: string;
  name: string;
  isAdmin: boolean;
  init: () => Promise<void>;
  /** A valid access token, refreshed when it is about to expire. */
  token: () => Promise<string>;
  logout: () => void;
};

let initPromise: Promise<void> | undefined; // keycloak.init must run only once (React StrictMode runs effects twice)

export const useAuth = create<AuthState>((set) => ({
  email: '',
  name: '',
  isAdmin: false,

  init: () =>
    (initPromise ??= keycloak
      .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
      .then(() => {
        const t = keycloak.tokenParsed as Claims | undefined;
        set({
          email: t?.email ?? '',
          name: t?.name ?? t?.preferred_username ?? '',
          isAdmin: !!t?.realm_access?.roles?.includes('admin'),
        });
      })),

  token: async () => {
    await keycloak.updateToken(30).catch(() => keycloak.login()); // session expired -> back to the login page
    return keycloak.token ?? '';
  },

  logout: () => {
    useCart.getState().clear();
    void keycloak.logout({ redirectUri: window.location.origin });
  },
}));
