export interface ReferenceDataMfeProps {
  basePath?: string;
  permissions?: readonly string[];
}

export const REF_HIERARCHY_MANAGE = 'REF_HIERARCHY_MANAGE';
export const REF_CATALOG_VIEW = 'REF_CATALOG_VIEW';
export const REF_CATALOG_MANAGE = 'REF_CATALOG_MANAGE';
export const REF_PARAMETER_VIEW = 'REF_PARAMETER_VIEW';
export const REF_PARAMETER_MANAGE = 'REF_PARAMETER_MANAGE';

/** UI capability only; the backend validates the token and resource scope. */
export const hasReferenceDataPermission = (permissions: readonly string[], code: string): boolean =>
  permissions.some((grant) => grant === code || (
    grant.startsWith(`${code}:`) &&
    /^(GLOBAL|TRAINING_CENTER|AREA|OWN_FICHAS|OWN_SCHEDULE|OWN_PROFILE|OWN_FICHA_AS_LEARNER)$/.test(grant.slice(code.length + 1))
  ));
