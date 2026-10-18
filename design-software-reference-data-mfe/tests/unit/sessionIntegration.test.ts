import { afterEach, expect, it, vi } from 'vitest';
import { requestReferenceData } from '../../src/adapter/out/http/referenceDataHttp.js';
import { hasReferenceDataPermission } from '../../src/contracts/remote-props.js';

afterEach(() => { sessionStorage.clear(); vi.unstubAllGlobals(); });

it('retries with the rotated host token after a 401, only once', async () => {
  sessionStorage.setItem('sena.access_token', 'expired-test-token');
  const fetchMock = vi.fn<typeof fetch>()
    .mockResolvedValueOnce(new Response('{}', { status: 401 }))
    .mockResolvedValueOnce(new Response('{"ok":true}'));
  vi.stubGlobal('fetch', fetchMock);
  const listener = (event: Event) => {
    (event as CustomEvent<{ promise?: Promise<void> }>).detail.promise = Promise.resolve().then(() => {
      sessionStorage.setItem('sena.access_token', 'rotated-test-token');
    });
  };
  window.addEventListener('sena:refresh-session', listener);
  try {
    expect(await requestReferenceData('/reference-data-api', '/macroregions')).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get('Authorization')).toBe('Bearer rotated-test-token');
  } finally { window.removeEventListener('sena:refresh-session', listener); }
});

it('uses the current shell token, preserves Headers and rewrites only through the proxy prefix', async () => {
  const fetchMock = vi.fn<typeof fetch>().mockImplementation(async () => new Response('{}'));
  vi.stubGlobal('fetch', fetchMock);
  sessionStorage.setItem('sena.access_token', 'test-session-one');
  await requestReferenceData('/reference-data-api', '/macroregions', { headers: new Headers({ 'X-Test': 'retained' }) });
  expect(String(fetchMock.mock.calls[0]?.[0])).toBe(`${window.location.origin}/reference-data-api/macroregions`);
  const headers = new Headers(fetchMock.mock.calls[0]?.[1]?.headers);
  expect(headers.get('Authorization')).toBe('Bearer test-session-one');
  expect(headers.get('X-Test')).toBe('retained');
  sessionStorage.removeItem('sena.access_token');
  await requestReferenceData('/reference-data-api', '/macroregions');
  expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).has('Authorization')).toBe(false);
});

it('recognizes scoped IAM grants without granting absent or malformed capabilities', () => {
  expect(hasReferenceDataPermission(['REF_HIERARCHY_MANAGE:GLOBAL'], 'REF_HIERARCHY_MANAGE')).toBe(true);
  expect(hasReferenceDataPermission(['REF_HIERARCHY_MANAGE:AREA'], 'REF_HIERARCHY_MANAGE')).toBe(true);
  expect(hasReferenceDataPermission(['REF_HIERARCHY_VIEW:GLOBAL'], 'REF_HIERARCHY_MANAGE')).toBe(false);
  expect(hasReferenceDataPermission(['REF_HIERARCHY_MANAGE:INVALID'], 'REF_HIERARCHY_MANAGE')).toBe(false);
  expect(hasReferenceDataPermission([], 'REF_HIERARCHY_MANAGE')).toBe(false);
});
