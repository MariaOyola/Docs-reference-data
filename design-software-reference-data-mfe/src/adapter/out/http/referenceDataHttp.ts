export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
  };
}

export class ReferenceDataHttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'ReferenceDataHttpError';
  }
}

export class MissingReferenceDataConfigurationError extends Error {
  constructor() {
    super('Falta configurar VITE_REFERENCE_DATA_API_BASE_URL.');
    this.name = 'MissingReferenceDataConfigurationError';
  }
}

const baseUrlWithSlash = (baseUrl: string): string =>
  baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`;

export const buildReferenceDataUrl = (baseUrl: string, path: string): URL => {
  const normalizedBaseUrl = baseUrl.trim();
  if (!normalizedBaseUrl) throw new MissingReferenceDataConfigurationError();
  const absoluteBase = new URL(baseUrlWithSlash(normalizedBaseUrl), window.location.origin);
  return new URL(path.replace(/^\//, ''), absoluteBase);
};

export async function requestReferenceData<T>(
  baseUrl: string,
  path: string,
  init: RequestInit = {},
  retried = false,
): Promise<T> {
  const url = buildReferenceDataUrl(baseUrl, path);
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  // Federated modules execute in the host origin: use the same live session as AuthClient.
  const token = sessionStorage.getItem('sena.access_token');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers,
    });
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') throw error;
    throw new Error(
      'No fue posible conectar con Reference Data. Verifica la conexión e intenta nuevamente.',
      { cause: error },
    );
  }

  if (!response.ok) {
    if (response.status === 401 && !retried) {
      const currentToken = sessionStorage.getItem('sena.access_token');
      if (currentToken && currentToken !== token) return requestReferenceData<T>(baseUrl, path, init, true);
      const detail: { promise?: Promise<void> } = {};
      window.dispatchEvent(new CustomEvent('sena:refresh-session', { detail }));
      if (detail.promise) {
        await detail.promise;
        return requestReferenceData<T>(baseUrl, path, init, true);
      }
    }
    if (response.status === 401 && retried) window.dispatchEvent(new Event('sena:session-expired'));
    let body: ApiErrorBody | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    throw new ReferenceDataHttpError(
      body?.error?.message ?? `La solicitud falló (HTTP ${response.status}).`,
      response.status,
      body?.error?.code,
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const jsonBody = (body: unknown): string => JSON.stringify(body);
