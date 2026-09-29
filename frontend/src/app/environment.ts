export const environment = { apiUrl: (globalThis as typeof globalThis & { GESTION_API_URL?: string }).GESTION_API_URL ?? 'http://localhost:8082/api/v1' };
