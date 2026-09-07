export async function fetchAuthStatus() {
  const res = await fetch('/auth/status');
  if (!res.ok) throw new Error('Failed to fetch auth status');
  return res.json();
}

export async function saveOAuthConfig(config) {
  const res = await fetch('/auth/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config)
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Failed to save config');
  }
  return res.json();
}

export async function getLoginUrl() {
  const res = await fetch('/auth/login');
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Failed to get login URL');
  }
  const data = await res.json();
  return data.url;
}

export async function submitManualToken(tokenData) {
  const res = await fetch('/auth/manual-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tokenData)
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Failed to submit token');
  }
  return res.json();
}

export async function logout() {
  const res = await fetch('/auth/logout', { method: 'POST' });
  return res.json();
}

export async function fetchCatalog() {
  const res = await fetch('/api/catalog');
  if (!res.ok) throw new Error('Failed to fetch catalog');
  return res.json();
}

export async function triggerCatalogRefresh() {
  const res = await fetch('/api/catalog/refresh', { method: 'POST' });
  if (!res.ok) throw new Error('Failed to refresh catalog');
  return res.json();
}

export async function fetchStorageInfo() {
  const res = await fetch('/api/storage');
  if (!res.ok) return null;
  return res.json();
}

export async function fetchFolderContents(folderId) {
  const res = await fetch(`/api/folder/${folderId}`);
  if (!res.ok) throw new Error('Failed to load folder');
  return res.json();
}
