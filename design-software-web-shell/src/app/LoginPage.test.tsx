import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthClient } from '@sena/frontend-platform';
import { HttpSessionDataSource } from './data-source/http-session-data-source';
import { ShellRoot } from './ShellRoot';
import { MockRemoteRegistry } from '../shell/remote/remote-registries';
import { loginReturnPath } from './LoginPage';

const me = { id: 'real-user', email: 'user@example.com', full_name: 'Usuario IAM', actor_type: 'USER', roles: [], features: ['REF_HIERARCHY_MANAGE:GLOBAL'], modules: ['MOD_REFERENCE_DATA'] };

const setup = (rejectLogin = false, rejectMe = false) => {
  let token: string | null = null;
  const fetchImpl = vi.fn(async (url: string, _init?: RequestInit) => {
    if (url.endsWith('/auth/login')) return rejectLogin
      ? new Response(JSON.stringify({ error_code: 'INVALID_CREDENTIALS', message: 'Invalid credentials' }), { status: 401 })
      : new Response(JSON.stringify({ access_token: 'test-access-token', token_type: 'Bearer', expires_in: 900 }));
    if (rejectMe) return new Response('{}', { status: 401 });
    return new Response(JSON.stringify(me));
  });
  const client = new AuthClient({ baseUrl: '/api/v1', fetchImpl, tokenProvider: {
    getAccessToken: () => token, setAccessToken: (value) => { token = value; }, clear: () => { token = null; },
  } });
  render(<MemoryRouter initialEntries={['/reference-data']}><ShellRoot dataSource={new HttpSessionDataSource(client)} remoteRegistry={new MockRemoteRegistry()} /></MemoryRouter>);
  return { fetchImpl, getToken: () => token };
};

const submit = async () => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('Correo electrónico'), 'user@example.com');
  await user.type(screen.getByLabelText('Contraseña'), 'test-password');
  await user.click(screen.getByRole('button', { name: 'Ingresar' }));
};

describe('login real del shell (transporte simulado en pruebas)', () => {
  it('redirects to the public form, stores the issued token, verifies me and returns to the remote', async () => {
    const { fetchImpl, getToken } = setup();
    await submit();
    expect(await screen.findByText('referenceDataMfe')).toBeInTheDocument();
    expect(getToken()).toBe('test-access-token');
    expect(fetchImpl.mock.calls[0]?.[0]).toBe('/api/v1/auth/login');
    expect(JSON.parse(String(fetchImpl.mock.calls[0]?.[1]?.body))).toEqual({ email: 'user@example.com', password: 'test-password' });
    expect(new Headers(fetchImpl.mock.calls[0]?.[1]?.headers).has('Authorization')).toBe(false);
    expect(new Headers(fetchImpl.mock.calls[1]?.[1]?.headers).get('Authorization')).toBe('Bearer test-access-token');
  });
  it('rejects invalid credentials without storing a token or retrying the password', async () => {
    const { fetchImpl, getToken } = setup(true);
    await submit();
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos');
    expect(fetchImpl).toHaveBeenCalledTimes(1);
    expect(getToken()).toBeNull();
    expect(screen.getByLabelText('Contraseña')).toHaveValue('');
  });
  it('clears the issued token if me rejects it', async () => {
    const { getToken } = setup(false, true);
    await submit();
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    expect(getToken()).toBeNull();
    expect(screen.queryByText('referenceDataMfe')).not.toBeInTheDocument();
  });
  it('restricts the return path to local routes and preserves filters', () => {
    expect(loginReturnPath({ from: '/reference-data?tab=geography' })).toBe('/reference-data?tab=geography');
    for (const from of ['https://evil.example', '//evil.example', '/\\evil.example', '/login?next=x']) {
      expect(loginReturnPath({ from })).toBe('/reference-data');
    }
  });
});
