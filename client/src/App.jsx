import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import HeroBanner from './components/HeroBanner';
import CategoryRow from './components/CategoryRow';
import ContinueWatching from './components/ContinueWatching';
import MediaCard from './components/MediaCard';
import SeriesModal from './components/SeriesModal';
import VideoPlayer from './components/VideoPlayer';
import SetupModal from './components/SetupModal';
import FolderBrowser from './components/FolderBrowser';
import {
  fetchAuthStatus,
  fetchCatalog,
  triggerCatalogRefresh,
  fetchStorageInfo,
  logout
} from './utils/api';
import { getWatchHistory } from './utils/storage';
import { Play, Sparkles, AlertCircle, RefreshCw, Film } from 'lucide-react';

export default function App() {
  const [authStatus, setAuthStatus] = useState(null);
  const [storageInfo, setStorageInfo] = useState(null);
  const [catalog, setCatalog] = useState({
    anime: [],
    cartoon: [],
    series: [],
    movies: [],
    news: [],
    otherFolders: [],
    allVideos: [],
    scanning: false
  });
  const [currentTab, setCurrentTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [activePlayerItem, setActivePlayerItem] = useState(null);
  const [seriesContext, setSeriesContext] = useState(null);
  const [selectedShow, setSelectedShow] = useState(null);
  const [isSetupOpen, setIsSetupOpen] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [watchHistory, setWatchHistory] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Load initial data and handle OAuth redirect params
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('auth_success')) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    initApp();
  }, []);

  const initApp = async () => {
    try {
      const status = await fetchAuthStatus();
      setAuthStatus(status);

      if (status.isAuthenticated) {
        await loadCatalogData();
        loadStorage();
      } else {
        // Open setup if not configured yet
        if (!status.isConfigured) {
          setIsSetupOpen(true);
        }
      }
    } catch (err) {
      console.error('Failed to initialize app:', err);
    } finally {
      setLoadingInitial(false);
      setWatchHistory(getWatchHistory());
    }
  };

  const loadCatalogData = async () => {
    try {
      const res = await fetchCatalog();
      if (res.catalog) {
        setCatalog(res.catalog);
      }
    } catch (err) {
      console.error('Error fetching catalog:', err);
    }
  };

  const loadStorage = async () => {
    try {
      const data = await fetchStorageInfo();
      if (data) setStorageInfo(data);
    } catch (_) {}
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      const res = await triggerCatalogRefresh();
      if (res.catalog) {
        setCatalog(res.catalog);
      }
      loadStorage();
    } catch (err) {
      console.error('Error refreshing catalog:', err);
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      setAuthStatus(prev => ({ ...prev, isAuthenticated: false, user: null }));
      setCatalog({
        anime: [],
        cartoon: [],
        series: [],
        movies: [],
        news: [],
        otherFolders: [],
        allVideos: [],
        scanning: false
      });
      setIsSetupOpen(true);
    } catch (err) {
      console.error(err);
    }
  };

  const handlePlayMedia = (item, context = null) => {
    setActivePlayerItem(item);
    setSeriesContext(context);
    setSelectedShow(null); // Close show modal if open
  };

  const handleOpenShow = (show) => {
    setSelectedShow(show);
  };

  // Pick a featured item for hero banner
  const getFeaturedItem = () => {
    if (catalog.movies && catalog.movies.length > 0) return catalog.movies[0];
    if (catalog.series && catalog.series.length > 0) return catalog.series[0];
    if (catalog.anime && catalog.anime.length > 0) return catalog.anime[0];
    if (catalog.cartoon && catalog.cartoon.length > 0) return catalog.cartoon[0];
    return null;
  };

  const featured = getFeaturedItem();

  // Filter items if searching
  const searchResults = searchQuery.trim()
    ? (catalog.allVideos || []).filter(v =>
        v.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.name?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : [];

  return (
    <div className="min-h-screen bg-[#141414] text-white flex flex-col">
      {/* Top Navigation */}
      <Navbar
        currentTab={currentTab}
        onSelectTab={(tab) => {
          setCurrentTab(tab);
          setSearchQuery('');
        }}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        authStatus={authStatus}
        storageInfo={storageInfo}
        onOpenSetup={() => setIsSetupOpen(true)}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing || catalog.scanning}
        onLogout={handleLogout}
      />

      {/* Main Content Area */}
      <main className="flex-1 pb-24 pt-16">
        {/* If Not Authenticated, show Welcome Connect Banner */}
        {!authStatus?.isAuthenticated && !loadingInitial && (
          <div className="mx-4 md:mx-10 mt-8 p-8 rounded-2xl bg-gradient-to-r from-brand-darkRed/40 via-neutral-900 to-neutral-900 border border-brand-red/30 shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="space-y-2 text-center md:text-left">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-brand-red text-white">
                <Sparkles className="w-3.5 h-3.5" /> Welcome to HuTube
              </div>
              <h2 className="text-2xl md:text-3xl font-black text-white">
                Connect your Google Drive to Start Streaming
              </h2>
              <p className="text-sm text-neutral-300 max-w-xl">
                HuTube bypasses Google Drive web playback limits and preview errors by streaming raw video chunks directly to your player. Connect your account to load your Anime, Series, Movies, Cartoons, and Funny Breaking News.
              </p>
            </div>
            <button
              onClick={() => setIsSetupOpen(true)}
              className="px-6 py-3 rounded-xl bg-brand-red hover:bg-brand-darkRed text-white font-bold text-sm shadow-xl shadow-brand-red/30 hover:scale-105 transition-all whitespace-nowrap"
            >
              Connect Google Drive
            </button>
          </div>
        )}

        {/* Searching Results View */}
        {searchQuery.trim() ? (
          <div className="px-4 md:px-10 pt-8">
            <h2 className="text-xl font-bold text-white mb-4">
              Search Results for "{searchQuery}" ({searchResults.length})
            </h2>

            {searchResults.length === 0 ? (
              <div className="text-center py-20 text-neutral-400">
                No matching videos found in your library.
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                {searchResults.map(item => (
                  <MediaCard
                    key={item.id}
                    item={item}
                    onPlay={handlePlayMedia}
                    onOpenShow={handleOpenShow}
                  />
                ))}
              </div>
            )}
          </div>
        ) : currentTab === 'browser' ? (
          /* Drive Folder Browser */
          <FolderBrowser
            otherFolders={catalog.otherFolders}
            onPlay={handlePlayMedia}
          />
        ) : currentTab === 'all' ? (
          /* Home Tab: Hero + Continue Watching + 5 Categories */
          <>
            {featured && (
              <HeroBanner
                featuredItem={featured}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />
            )}

            <div className="relative z-20 -mt-10 md:-mt-16 space-y-4">
              <ContinueWatching
                history={watchHistory}
                onPlay={handlePlayMedia}
                onUpdateHistory={() => setWatchHistory(getWatchHistory())}
              />

              <CategoryRow
                title="Anime"
                icon="🎌"
                items={catalog.anime}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />

              <CategoryRow
                title="Movies"
                icon="🎬"
                items={catalog.movies}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />

              <CategoryRow
                title="Series"
                icon="📺"
                items={catalog.series}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />

              <CategoryRow
                title="Cartoon"
                icon="🎨"
                items={catalog.cartoon}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />

              <CategoryRow
                title="Funny Breaking News"
                icon="📰"
                items={catalog.news}
                onPlay={handlePlayMedia}
                onOpenShow={handleOpenShow}
              />
            </div>
          </>
        ) : (
          /* Specific Category Tab (Anime, Movies, Series, Cartoon, News) */
          <div className="px-4 md:px-10 pt-6">
            <div className="flex items-center justify-between mb-6">
              <h1 className="text-2xl md:text-3xl font-black capitalize text-white flex items-center gap-2">
                {currentTab === 'anime' && '🎌 Anime'}
                {currentTab === 'movies' && '🎬 Movies'}
                {currentTab === 'series' && '📺 Series'}
                {currentTab === 'cartoon' && '🎨 Cartoon'}
                {currentTab === 'news' && '📰 Funny Breaking News'}
              </h1>
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-neutral-800 text-neutral-300">
                {catalog[currentTab]?.length || 0} Titles
              </span>
            </div>

            {catalog[currentTab]?.length === 0 ? (
              <div className="text-center py-20 text-neutral-400">
                {catalog.scanning ? (
                  <div className="flex items-center justify-center gap-2">
                    <RefreshCw className="w-5 h-5 animate-spin text-brand-red" />
                    <span>Scanning Google Drive for files...</span>
                  </div>
                ) : (
                  <div>
                    <p className="text-base font-semibold text-neutral-300">No media found in this folder yet.</p>
                    <p className="text-xs text-neutral-500 mt-1">Make sure you have a folder named "{currentTab}" in your Google Drive and hit refresh.</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                {catalog[currentTab].map(item => (
                  <MediaCard
                    key={item.id}
                    item={item}
                    onPlay={handlePlayMedia}
                    onOpenShow={handleOpenShow}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Series / Show Episodes Modal */}
      {selectedShow && (
        <SeriesModal
          show={selectedShow}
          onClose={() => setSelectedShow(null)}
          onPlayEpisode={(ep, show) => handlePlayMedia(ep, show)}
        />
      )}

      {/* Video Player Modal */}
      {activePlayerItem && (
        <VideoPlayer
          item={activePlayerItem}
          seriesContext={seriesContext}
          onClose={() => {
            setActivePlayerItem(null);
            setWatchHistory(getWatchHistory());
          }}
          onPlayNext={(nextEp) => handlePlayMedia(nextEp, seriesContext)}
        />
      )}

      {/* Setup Wizard Modal */}
      <SetupModal
        isOpen={isSetupOpen}
        onClose={() => setIsSetupOpen(false)}
        authStatus={authStatus}
        onAuthSuccess={initApp}
      />
    </div>
  );
}
