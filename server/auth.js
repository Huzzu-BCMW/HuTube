const fs = require('fs');
const path = require('path');
const { google } = require('googleapis');

const CONFIG_PATH = path.join(__dirname, 'config.json');
const TOKENS_PATH = path.join(__dirname, 'tokens.json');

const SCOPES = [
  'https://www.googleapis.com/auth/drive.readonly',
  'https://www.googleapis.com/auth/userinfo.profile',
  'https://www.googleapis.com/auth/userinfo.email'
];

function readJSON(filePath, fallback = {}) {
  try {
    if (fs.existsSync(filePath)) {
      const data = fs.readFileSync(filePath, 'utf8');
      return JSON.parse(data);
    }
  } catch (err) {
    console.error(`Error reading ${filePath}:`, err.message);
  }
  return fallback;
}

function writeJSON(filePath, data) {
  try {
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error(`Error writing ${filePath}:`, err.message);
    return false;
  }
}

class AuthManager {
  constructor() {
    this.config = readJSON(CONFIG_PATH, {
      clientId: process.env.GOOGLE_CLIENT_ID || '',
      clientSecret: process.env.GOOGLE_CLIENT_SECRET || '',
      redirectUri: process.env.GOOGLE_REDIRECT_URI || 'http://localhost:5000/auth/callback'
    });
    this.tokens = readJSON(TOKENS_PATH, null);
    this.oauth2Client = null;
    this.userProfile = null;
    this.initOAuthClient();
  }

  initOAuthClient() {
    if (this.config.clientId && this.config.clientSecret) {
      this.oauth2Client = new google.auth.OAuth2(
        this.config.clientId,
        this.config.clientSecret,
        this.config.redirectUri
      );

      if (this.tokens) {
        this.oauth2Client.setCredentials(this.tokens);
        this.oauth2Client.on('tokens', (newTokens) => {
          this.tokens = { ...this.tokens, ...newTokens };
          writeJSON(TOKENS_PATH, this.tokens);
          console.log('[Auth] Refreshed and saved access token');
        });
      }
    }
  }

  saveConfig(newConfig) {
    this.config = {
      ...this.config,
      ...newConfig
    };
    writeJSON(CONFIG_PATH, this.config);
    this.initOAuthClient();
    return this.config;
  }

  saveTokens(tokens) {
    this.tokens = tokens;
    writeJSON(TOKENS_PATH, tokens);
    this.initOAuthClient();
  }

  getAuthUrl() {
    if (!this.oauth2Client) {
      throw new Error('Google OAuth Client ID & Secret have not been configured yet.');
    }
    return this.oauth2Client.generateAuthUrl({
      access_type: 'offline',
      prompt: 'consent',
      scope: SCOPES
    });
  }

  async handleCallback(code) {
    if (!this.oauth2Client) {
      throw new Error('Google OAuth Client not initialized');
    }
    const { tokens } = await this.oauth2Client.getToken(code);
    this.saveTokens(tokens);
    await this.fetchUserProfile();
    return tokens;
  }

  async fetchUserProfile() {
    try {
      if (!this.oauth2Client || !this.tokens) return null;
      const oauth2 = google.oauth2({ version: 'v2', auth: this.oauth2Client });
      const { data } = await oauth2.userinfo.get();
      this.userProfile = data;
      return data;
    } catch (err) {
      console.warn('[Auth] Could not fetch user profile:', err.message);
      return null;
    }
  }

  async getValidToken() {
    if (!this.oauth2Client || !this.tokens) {
      return null;
    }
    try {
      const tokenResponse = await this.oauth2Client.getAccessToken();
      return tokenResponse.token || this.tokens.access_token;
    } catch (err) {
      console.error('[Auth] Error getting valid access token:', err.message);
      return this.tokens?.access_token || null;
    }
  }

  async getStatus() {
    const isConfigured = !!(this.config.clientId && this.config.clientSecret);
    const isAuthenticated = !!(this.tokens && (this.tokens.access_token || this.tokens.refresh_token));
    
    if (isAuthenticated && !this.userProfile) {
      await this.fetchUserProfile();
    }

    return {
      isConfigured,
      isAuthenticated,
      user: this.userProfile,
      clientIdMasked: this.config.clientId ? `${this.config.clientId.substring(0, 8)}...` : null,
      redirectUri: this.config.redirectUri
    };
  }

  logout() {
    this.tokens = null;
    this.userProfile = null;
    try {
      if (fs.existsSync(TOKENS_PATH)) {
        fs.unlinkSync(TOKENS_PATH);
      }
    } catch (err) {
      console.error('Error deleting tokens file:', err.message);
    }
    this.initOAuthClient();
    return true;
  }
}

module.exports = new AuthManager();
