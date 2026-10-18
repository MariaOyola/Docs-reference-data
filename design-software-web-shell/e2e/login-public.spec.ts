import { expect, test } from '@playwright/test';

test.use({ channel: process.env['PLAYWRIGHT_CHANNEL'] });

test('serves the real public login while other domain remotes are unavailable', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/reference-data');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  await expect(page.getByLabel('Correo electrónico')).toHaveAttribute('type', 'email');
  await expect(page.getByLabel('Contraseña')).toHaveAttribute('type', 'password');
  await expect(page.getByRole('button', { name: 'Ingresar' })).toBeEnabled();
  expect(await page.evaluate(() => sessionStorage.getItem('sena.access_token'))).toBeNull();
  expect(errors).toEqual([]);
  await page.screenshot({ path: 'test-results/login-public.png', fullPage: true });
});
