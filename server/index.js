const express = require('express');
const cors = require('cors');
const path = require('path');
const axios = require('axios');
require('dotenv').config();

const authManager = require('./auth');
const driveClient = require('./driveClient');
const driveScanner = require('./driveScanner');
const { handleStream, handleM3uPlaylist } = require('./streamer');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- AUTHENTICATION ROUTES ---

app.get('/auth/status', async (req, res) => {
  try {
    const status = await authManager.getStatus();
    res.json(status);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/auth/config', (req, res) => {
  const { clientId, clientSecret, redirectUri } = req.body;
  if (!clientId || !clientSecret) {
    return res.status(400).json({ error: 'Client ID and Client Secret are required' });
  }
  const config = authManager.saveConfig({ clientId, clientSecret, redirectUri });
  res.json({ success: true, config });
});

app.get('/auth/login', (req, res) => {
  try {
    const url = authManager.getAuthUrl();
    res.json({ url });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.get('/auth/callback', async (req, res) => {
  const { code, error } = req.query;
  if (error) {
    return res.redirect(`http://localhost:3000/?auth_error=${encodeURIComponent(error)}`);
  }
  if (!code) {
    return res.status(400).send('Missing code parameter');
  }

  try {
    await authManager.handleCallback(code);
    console.log('[Auth] Google Drive account successfully authenticated!');
    
    // Trigger background catalog scan
    driveScanner.scanAll().catch(e => console.error('[Scanner] Initial scan error:', e.message));

    // Redirect to frontend with success
    res.redirect('http://localhost:3000/?auth_success=1');
  } catch (err) {
    console.error('[Auth] Callback error:', err.message);
    res.redirect(`http://localhost:3000/?auth_error=${encodeURIComponent(err.message)}`);
  }
});

app.post('/auth/manual-token', (req, res) => {
  const { access_token, refresh_token } = req.body;
  if (!access_token && !refresh_token) {
    return res.status(400).json({ error: 'Token is required' });
  }
  authManager.saveTokens({ access_token, refresh_token });
  driveScanner.scanAll().catch(e => console.error('[Scanner] Initial scan error:', e.message));
  res.json({ success: true });
});

app.post('/auth/logout', (req, res) => {
  authManager.logout();
  res.json({ success: true });
});

// --- CATALOG & DRIVE ROUTES ---

app.get('/api/catalog', async (req, res) => {
  try {
    const status = await authManager.getStatus();
    if (!status.isAuthenticated) {
      return res.json({
        authenticated: false,
        catalog: driveScanner.getCatalog()
      });
    }

    const currentCatalog = driveScanner.getCatalog();
    // If not scanned yet, trigger scan
    if (!currentCatalog.lastScanned && !currentCatalog.scanning) {
      driveScanner.scanAll().catch(err => console.error('[Scanner] Background scan error:', err.message));
    }

    res.json({
      authenticated: true,
      catalog: currentCatalog
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/catalog/refresh', async (req, res) => {
  try {
    const catalog = await driveScanner.scanAll();
    res.json({ success: true, catalog });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/folder/:folderId', async (req, res) => {
  try {
    const items = await driveClient.listFolderChildren(req.params.folderId);
    res.json({ files: items });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/storage', async (req, res) => {
  try {
    const about = await driveClient.getAbout();
    res.json(about);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- STREAMING ROUTES ---

app.get('/api/stream/:fileId', handleStream);
app.get('/api/stream/:fileId/playlist.m3u', handleM3uPlaylist);

// --- THUMBNAIL PROXY ROUTE ---

app.get('/api/thumbnail/:fileId', async (req, res) => {
  try {
    const token = await authManager.getValidToken();
    if (!token) {
      return res.status(401).send('Not authenticated');
    }
    const { fileId } = req.params;
    const thumbUrl = `https://drive.google.com/thumbnail?id=${fileId}&sz=w800`;

    const response = await axios.get(thumbUrl, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'stream',
      validateStatus: (s) => s < 400
    });

    res.setHeader('Content-Type', response.headers['content-type'] || 'image/jpeg');
    res.setHeader('Cache-Control', 'public, max-age=86400');
    response.data.pipe(res);
  } catch (err) {
    // If thumbnail fails, return 404
    res.status(404).send('Thumbnail not found');
  }
});

// --- SERVE FRONTEND (in production) ---

const clientDist = path.join(__dirname, '../client/dist');
app.use(express.static(clientDist));

app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api') || req.path.startsWith('/auth')) {
    return next();
  }
  const indexHtml = path.join(clientDist, 'index.html');
  if (require('fs').existsSync(indexHtml)) {
    res.sendFile(indexHtml);
  } else {
    next();
  }
});

app.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(`🍿 HuTube Server running on port ${PORT}`);
  console.log(`📡 Stream Proxy: http://localhost:${PORT}/api/stream/:fileId`);
  console.log(`🌐 Open Client:  http://localhost:3000`);
  console.log(`=========================================`);
});
