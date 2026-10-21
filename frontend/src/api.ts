import type { Cart, LogEntry } from './types';

// The one place in the app that calls fetch. Everything the demo wants to show about a
// request - its status and which instance answered it - is captured here, so no caller can
// make an API call that silently escapes the request log.

export type ApiResult<T> = {
  status: number;
  instanceId: string | null;
  body: T | null;
};

export type LogListener = (entry: Omit<LogEntry, 'id'>) => void;

let listener: LogListener = () => {};

export function onRequest(next: LogListener): void {
  listener = next;
}

// Same-origin paths: nginx (or the Vite dev proxy) strips /api and forwards to the service.
// No credentials option needed - the SESSION cookie is first-party here.
async function call<T>(method: string, path: string, body?: unknown): Promise<ApiResult<T>> {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const instanceId = response.headers.get('X-Instance-Id');
  listener({
    at: new Date().toLocaleTimeString(),
    method,
    path,
    status: response.status,
    instanceId,
  });

  // login and logout answer 200 with an empty body; JSON.parse would throw on that.
  const text = await response.text();
  return {
    status: response.status,
    instanceId,
    body: text.length === 0 ? null : (JSON.parse(text) as T),
  };
}

export type ErrorBody = { error: string };
export type SessionBody = { username: string };

export const api = {
  login: (username: string, password: string) =>
    call<ErrorBody>('POST', '/login', { username, password }),
  logout: () => call<never>('POST', '/logout'),
  session: () => call<SessionBody | ErrorBody>('GET', '/session'),
  viewCart: () => call<Cart | ErrorBody>('GET', '/cart'),
  addItem: (item: string) => call<Cart | ErrorBody>('POST', '/cart/add', { item }),
};
