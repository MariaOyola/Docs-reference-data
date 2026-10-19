import { useState, type FormEvent } from 'react';
import { HttpError } from '@sena/frontend-platform';
import type { SessionDataSource } from './data-source/session-data-source';
import './login.css';


// Recibe correo y contraseña del formulario y solicita iniciar sesión.

export const loginReturnPath = (state: unknown): string => {
  const from = typeof state === 'object' && state !== null && 'from' in state ? state.from : null;
  if (typeof from !== 'string' || !from.startsWith('/') || /^\/[\/\\]/.test(from) || /[\\\r\n]/.test(from)) return '/reference-data';
  return from.split(/[?#]/)[0] === '/login' ? '/reference-data' : from;
};

export const LoginPage = ({ dataSource, onAuthenticated }: {
  dataSource: SessionDataSource;
  onAuthenticated: () => Promise<void>;
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      if (dataSource.kind !== 'http' || !dataSource.login) throw new Error('El inicio de sesión HTTP no está configurado.');
      await dataSource.login(email.trim(), password);
      setPassword('');
      await onAuthenticated();
    } catch (cause) {
      setPassword('');
      setError(cause instanceof HttpError
        ? cause.status === 401 ? 'Correo o contraseña incorrectos.'
          : cause.status === 429 ? 'Demasiados intentos. Espera antes de volver a intentarlo.'
            : `IAM no pudo iniciar la sesión (HTTP ${cause.status}). ${cause.message}`
        : 'No se pudo iniciar sesión. Comprueba la conexión con IAM e inténtalo de nuevo.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={(event) => void submit(event)} aria-busy={busy}>
        <p className="login-brand">SENA</p>
        <h1>Iniciar sesión</h1>
        <p>Ingresa con tu cuenta institucional.</p>
        <label htmlFor="login-email">Correo electrónico</label>
        <input id="login-email" name="email" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} disabled={busy} />
        <label htmlFor="login-password">Contraseña</label>
        <input id="login-password" name="password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} disabled={busy} />
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={busy}>{busy ? 'Iniciando sesión…' : 'Ingresar'}</button>
      </form>
    </main>
  );
};
