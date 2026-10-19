/// <reference types="vite/client" />

// En desarrollo, dirige las peticiones del navegador hacia IAM o Reference Data mediante proxies.

interface ImportMetaEnv {
  readonly VITE_SESSION_SOURCE?: 'mock' | 'http';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}