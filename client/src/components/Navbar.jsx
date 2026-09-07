import React, { useState } from 'react';
import { Play, Search, RefreshCw, HardDrive, User, Settings, LogOut, CheckCircle2, AlertCircle } from 'lucide-react';

export default function Navbar({
  currentTab,
  onSelectTab,
  searchQuery,
  onSearchChange,
  authStatus,
  storageInfo,
  onOpenSetup,
  onRefresh,
  isRefreshing,
  onLogout
}) {
  const [showUserDropdown, setShowUserDropdown] = useState(false);

  const tabs = [
    { id: 'all', label: 'Home' },
    { id: 'anime', label: 'Anime' },
    { id: 'series', label: 'Series' },
    { id: 'movies', label: 'Movies' },
    { id: 'cartoon', label: 'Cartoon' },
    { id: 'news', label: 'Funny News' },
    { id: 'browser', label: 'Drive Files' },
  ];

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 GB';
    const gb = bytes / (1024 * 1024 * 1024);
    return `${gb.toFixed(1)} GB`;
  };

  const quotaPercent = storageInfo?.storageQuota?.limit
    ? Math.round((storageInfo.storageQuota.usage / storageInfo.storageQuota.limit) * 100)
    : null;

  return (
    <nav className="fixed top-0 left-0 right-0 z-40 bg-gradient-to-b from-black/90 via-black/70 to-transparent backdrop-blur-md px-4 md:px-10 py-3 transition-all">
      <div className="flex items-center justify-between gap-4">
        {/* Left: Brand Logo & Links */}
        <div className="flex items-center gap-8">
          <div 
            onClick={() => onSelectTab('all')}
            className="flex items-center gap-2 cursor-pointer group select-none"
          >
            <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-brand-darkRed to-brand-red flex items-center justify-center shadow-lg shadow-brand-red/30 group-hover:scale-105 transition-transform">
              <Play className="w-5 h-5 text-white fill-white ml-0.5" />
            </div>
            <div className="flex items-baseline">
              <span className="text-2xl font-black tracking-tight text-white group-hover:text-red-400 transition-colors">Hu</span>
              <span className="text-2xl font-black tracking-tight text-brand-red">Tube</span>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="hidden lg:flex items-center gap-1">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => onSelectTab(tab.id)}
                className={`px-3.5 py-1.5 rounded-full text-sm font-medium transition-all ${
                  currentTab === tab.id
                    ? 'bg-white text-black shadow-md font-semibold'
                    : 'text-neutral-300 hover:text-white hover:bg-white/10'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Right: Search, Storage, Sync, User */}
        <div className="flex items-center gap-3">
          {/* Search Bar */}
          <div className="relative flex items-center">
            <div className="relative flex items-center">
              <input
                type="text"
                placeholder="Search titles, shows..."
                value={searchQuery}
                onChange={(e) => onSearchChange(e.target.value)}
                className="w-36 md:w-64 bg-neutral-900/80 hover:bg-neutral-900 focus:bg-neutral-950 text-xs md:text-sm text-white placeholder-neutral-400 pl-9 pr-4 py-1.5 rounded-full border border-neutral-700/60 focus:border-brand-red focus:outline-none focus:ring-1 focus:ring-brand-red transition-all"
              />
              <Search className="w-4 h-4 text-neutral-400 absolute left-3 pointer-events-none" />
            </div>
          </div>

          {/* Drive Storage Quota Indicator (if authenticated) */}
          {storageInfo?.storageQuota?.limit && (
            <div className="hidden xl:flex items-center gap-2 px-3 py-1 bg-neutral-900/70 border border-neutral-800 rounded-full text-xs text-neutral-300" title={`Used ${formatBytes(storageInfo.storageQuota.usage)} of ${formatBytes(storageInfo.storageQuota.limit)}`}>
              <HardDrive className="w-3.5 h-3.5 text-neutral-400" />
              <span>{formatBytes(storageInfo.storageQuota.usage)} / {formatBytes(storageInfo.storageQuota.limit)}</span>
              <div className="w-12 h-1.5 bg-neutral-800 rounded-full overflow-hidden">
                <div 
                  className={`h-full rounded-full ${quotaPercent > 90 ? 'bg-red-500' : 'bg-brand-red'}`} 
                  style={{ width: `${Math.min(100, quotaPercent || 0)}%` }} 
                />
              </div>
            </div>
          )}

          {/* Refresh / Rescan Catalog Button */}
          {authStatus?.isAuthenticated && (
            <button
              onClick={onRefresh}
              disabled={isRefreshing}
              title="Refresh Google Drive Library"
              className="p-2 rounded-full text-neutral-300 hover:text-white hover:bg-neutral-800/80 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-brand-red' : ''}`} />
            </button>
          )}

          {/* Connection Status Button */}
          {authStatus?.isAuthenticated ? (
            <div className="relative">
              <button
                onClick={() => setShowUserDropdown(!showUserDropdown)}
                className="flex items-center gap-2 p-1.5 pr-2.5 rounded-full bg-neutral-800/80 hover:bg-neutral-800 border border-neutral-700/60 transition-colors"
              >
                {authStatus.user?.picture ? (
                  <img
                    src={authStatus.user.picture}
                    alt="avatar"
                    className="w-6 h-6 rounded-full ring-1 ring-white/20"
                  />
                ) : (
                  <div className="w-6 h-6 rounded-full bg-brand-red text-white text-xs flex items-center justify-center font-bold">
                    {authStatus.user?.name ? authStatus.user.name[0] : 'U'}
                  </div>
                )}
                <span className="hidden sm:inline text-xs font-medium max-w-[100px] truncate">
                  {authStatus.user?.name || 'Connected'}
                </span>
                <div className="w-2 h-2 rounded-full bg-green-500 ring-2 ring-neutral-900" />
              </button>

              {/* Dropdown Menu */}
              {showUserDropdown && (
                <div 
                  className="absolute right-0 mt-2 w-56 glass-dropdown rounded-xl p-2 shadow-2xl z-50 animate-in fade-in slide-in-from-top-2"
                  onClick={() => setShowUserDropdown(false)}
                >
                  <div className="px-3 py-2 border-b border-neutral-800 mb-1">
                    <p className="text-xs font-semibold text-white truncate">{authStatus.user?.name || 'Google User'}</p>
                    <p className="text-[11px] text-neutral-400 truncate">{authStatus.user?.email || 'Connected to Drive'}</p>
                  </div>
                  <button
                    onClick={onOpenSetup}
                    className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-neutral-300 hover:text-white hover:bg-neutral-800 transition-colors text-left"
                  >
                    <Settings className="w-4 h-4 text-neutral-400" />
                    <span>Drive Connection Settings</span>
                  </button>
                  <button
                    onClick={onLogout}
                    className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-red-400 hover:text-red-300 hover:bg-red-950/40 transition-colors text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Disconnect Account</span>
                  </button>
                </div>
              )}
            </div>
          ) : (
            <button
              onClick={onOpenSetup}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-brand-red hover:bg-brand-darkRed text-white rounded-full text-xs font-semibold transition-all shadow-md shadow-brand-red/20 glow-red"
            >
              <AlertCircle className="w-3.5 h-3.5" />
              <span>Connect Drive</span>
            </button>
          )}
        </div>
      </div>

      {/* Mobile Category Tab Bar */}
      <div className="flex lg:hidden items-center gap-1 mt-2.5 overflow-x-auto no-scrollbar py-1">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => onSelectTab(tab.id)}
            className={`px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
              currentTab === tab.id
                ? 'bg-white text-black font-semibold'
                : 'text-neutral-400 hover:text-white bg-neutral-900/60'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </nav>
  );
}
