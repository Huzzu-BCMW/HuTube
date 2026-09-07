import React, { useState, useEffect } from 'react';
import { X, Key, ShieldCheck, ExternalLink, Copy, Check, AlertCircle, LogIn, HardDrive } from 'lucide-react';
import { saveOAuthConfig, getLoginUrl, submitManualToken } from '../utils/api';

export default function SetupModal({ isOpen, onClose, authStatus, onAuthSuccess }) {
  const [activeTab, setActiveTab] = useState('oauth'); // 'oauth' | 'manual'
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [refreshToken, setRefreshToken] = useState('');
  const [copiedRedirect, setCopiedRedirect] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const redirectUri = authStatus?.redirectUri || 'http://localhost:5000/auth/callback';

  useEffect(() => {
    setErrorMsg(null);
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCopyRedirect = () => {
    navigator.clipboard.writeText(redirectUri);
    setCopiedRedirect(true);
    setTimeout(() => setCopiedRedirect(false), 2000);
  };

  const handleSaveAndLogin = async (e) => {
    e.preventDefault();
    setErrorMsg(null);
    setLoading(true);

    try {
      if (clientId && clientSecret) {
        await saveOAuthConfig({ clientId, clientSecret, redirectUri });
      }

      // Fetch the login URL from the backend
      const loginUrl = await getLoginUrl();
      // Open Google OAuth consent in the same or new window
      window.location.href = loginUrl;
    } catch (err) {
      setErrorMsg(err.message);
      setLoading(false);
    }
  };

  const handleManualTokenSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg(null);
    setLoading(true);

    try {
      await submitManualToken({
        access_token: accessToken.trim(),
        refresh_token: refreshToken.trim()
      });
      if (onAuthSuccess) onAuthSuccess();
      onClose();
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        className="relative w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden shadow-2xl p-6 md:p-8"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-brand-red/20 text-brand-red flex items-center justify-center">
            <Key className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Connect Google Drive</h2>
            <p className="text-xs text-neutral-400">Stream your movies and series directly without playback errors</p>
          </div>
        </div>

        {/* Connection Status Banner */}
        {authStatus?.isAuthenticated && (
          <div className="mb-5 p-3 rounded-xl bg-green-950/40 border border-green-800/60 flex items-center gap-3 text-xs text-green-300">
            <ShieldCheck className="w-5 h-5 text-green-400 flex-none" />
            <div>
              <span className="font-semibold">Connected to Google Drive!</span>
              <p className="text-green-400/80 mt-0.5">{authStatus.user?.email || 'Authenticated'}</p>
            </div>
          </div>
        )}

        {/* Tab selection */}
        <div className="flex items-center gap-2 border-b border-neutral-800 mb-5 pb-2">
          <button
            onClick={() => setActiveTab('oauth')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === 'oauth'
                ? 'bg-neutral-800 text-white'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Google Cloud OAuth (Recommended)
          </button>
          <button
            onClick={() => setActiveTab('manual')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === 'manual'
                ? 'bg-neutral-800 text-white'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Direct Token Input
          </button>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3 rounded-lg bg-red-950/50 border border-red-800 text-xs text-red-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-none" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Tab 1: OAuth Flow */}
        {activeTab === 'oauth' && (
          <form onSubmit={handleSaveAndLogin} className="space-y-4">
            {/* Quick 3-step Instructions */}
            <div className="p-3.5 rounded-xl bg-neutral-950 border border-neutral-800 text-xs space-y-2 text-neutral-300">
              <span className="font-bold text-white block">Quick 2-Minute Setup:</span>
              <ol className="list-decimal list-inside space-y-1 text-neutral-400">
                <li>
                  Open <a href="https://console.cloud.google.com/apis/credentials" target="_blank" rel="noreferrer" className="text-brand-red underline inline-flex items-center gap-0.5">Google Cloud Credentials <ExternalLink className="w-3 h-3" /></a>
                </li>
                <li>Create an <strong>OAuth 2.0 Client ID</strong> (Application type: <em>Web application</em>).</li>
                <li>Add this exact <strong>Authorized redirect URI</strong>:</li>
              </ol>

              {/* Copyable Redirect URI Box */}
              <div className="flex items-center justify-between p-2 rounded-lg bg-neutral-900 border border-neutral-700/80 font-mono text-[11px] text-neutral-200">
                <span className="truncate mr-2">{redirectUri}</span>
                <button
                  type="button"
                  onClick={handleCopyRedirect}
                  className="flex items-center gap-1 text-xs text-brand-red hover:text-red-400 font-sans font-semibold flex-none"
                >
                  {copiedRedirect ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedRedirect ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>

            {/* Inputs */}
            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1">
                Client ID
              </label>
              <input
                type="text"
                placeholder="123456789-xxxx.apps.googleusercontent.com"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                required={!authStatus?.isConfigured}
                className="w-full px-3.5 py-2 rounded-lg bg-neutral-950 border border-neutral-700 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-brand-red"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1">
                Client Secret
              </label>
              <input
                type="password"
                placeholder="GOCSPX-xxxxxxxxxxxx"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
                required={!authStatus?.isConfigured}
                className="w-full px-3.5 py-2 rounded-lg bg-neutral-950 border border-neutral-700 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-brand-red"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg bg-brand-red hover:bg-brand-darkRed text-white font-bold text-sm transition-all shadow-lg shadow-brand-red/30 disabled:opacity-50"
            >
              <LogIn className="w-4 h-4" />
              <span>{loading ? 'Connecting...' : 'Sign in with Google'}</span>
            </button>
          </form>
        )}

        {/* Tab 2: Direct Token Input */}
        {activeTab === 'manual' && (
          <form onSubmit={handleManualTokenSubmit} className="space-y-4">
            <p className="text-xs text-neutral-400">
              If you already have an OAuth token (e.g. from Google OAuth Playground), paste it here directly:
            </p>

            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1">
                Access Token (or Refresh Token)
              </label>
              <textarea
                rows="3"
                placeholder="ya29.a0AfH6..."
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
                required
                className="w-full px-3.5 py-2 rounded-lg bg-neutral-950 border border-neutral-700 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-brand-red font-mono"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-lg bg-brand-red hover:bg-brand-darkRed text-white font-bold text-sm transition-all shadow-lg disabled:opacity-50"
            >
              {loading ? 'Saving Token...' : 'Save & Connect'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
