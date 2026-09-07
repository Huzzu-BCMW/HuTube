import React, { useState, useEffect } from 'react';
import { Folder, Film, ChevronRight, ArrowLeft, Play, ExternalLink, RefreshCw } from 'lucide-react';
import { fetchFolderContents } from '../utils/api';

export default function FolderBrowser({ onPlay, otherFolders = [] }) {
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const [currentFolder, setCurrentFolder] = useState(null);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadFolder = async (folder) => {
    setLoading(true);
    try {
      const data = await fetchFolderContents(folder.id);
      setItems(data.files || []);
      setCurrentFolder(folder);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenFolder = (folder) => {
    setBreadcrumbs([...breadcrumbs, folder]);
    loadFolder(folder);
  };

  const handleNavigateBreadcrumb = (idx) => {
    if (idx === -1) {
      // Root
      setBreadcrumbs([]);
      setCurrentFolder(null);
      setItems([]);
    } else {
      const target = breadcrumbs[idx];
      setBreadcrumbs(breadcrumbs.slice(0, idx + 1));
      loadFolder(target);
    }
  };

  return (
    <div className="px-4 md:px-10 py-6 max-w-7xl mx-auto">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-2 mb-6 text-sm text-neutral-400 overflow-x-auto no-scrollbar py-1">
        <button
          onClick={() => handleNavigateBreadcrumb(-1)}
          className="hover:text-white font-semibold transition-colors whitespace-nowrap"
        >
          Drive Folders
        </button>
        {breadcrumbs.map((b, idx) => (
          <React.Fragment key={b.id || idx}>
            <ChevronRight className="w-4 h-4 text-neutral-600 flex-none" />
            <button
              onClick={() => handleNavigateBreadcrumb(idx)}
              className={`hover:text-white transition-colors whitespace-nowrap ${
                idx === breadcrumbs.length - 1 ? 'text-brand-red font-bold' : ''
              }`}
            >
              {b.name}
            </button>
          </React.Fragment>
        ))}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20 text-neutral-400 gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-brand-red" />
          <span>Loading folder contents...</span>
        </div>
      ) : !currentFolder ? (
        /* Root Folders List */
        <div className="space-y-4">
          <h2 className="text-xl font-bold text-white mb-4">All Google Drive Folders</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {otherFolders.map((folder) => (
              <div
                key={folder.id}
                onClick={() => handleOpenFolder(folder)}
                className="group flex flex-col items-center justify-center p-6 rounded-xl bg-neutral-900 border border-neutral-800 hover:border-neutral-600 hover:bg-neutral-850 cursor-pointer transition-all hover:scale-105"
              >
                <Folder className="w-12 h-12 text-yellow-500 mb-2 group-hover:scale-110 transition-transform" />
                <span className="text-xs md:text-sm font-semibold text-white text-center line-clamp-2">
                  {folder.name}
                </span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        /* Current Folder Items */
        <div className="space-y-3">
          <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
            <h3 className="text-lg font-bold text-white">{currentFolder.name}</h3>
            <span className="text-xs text-neutral-400">{items.length} items</span>
          </div>

          {items.length === 0 ? (
            <div className="text-center py-16 text-neutral-500 text-sm">
              Folder is empty.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {items.map((item) => {
                const isDir = item.mimeType === 'application/vnd.google-apps.folder';
                const isVideo = item.mimeType?.startsWith('video/') || item.name?.match(/\.(mp4|mkv|webm|avi|mov|ts)$/i);

                if (isDir) {
                  return (
                    <div
                      key={item.id}
                      onClick={() => handleOpenFolder(item)}
                      className="flex items-center gap-3 p-3 rounded-xl bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 hover:border-neutral-700 cursor-pointer transition-all"
                    >
                      <Folder className="w-6 h-6 text-yellow-500 flex-none" />
                      <span className="text-xs font-semibold text-white truncate">{item.name}</span>
                    </div>
                  );
                }

                if (isVideo) {
                  const mediaItem = {
                    id: item.id,
                    title: item.name.replace(/\.[^/.]+$/, ''),
                    name: item.name,
                    thumbnailLink: item.thumbnailLink,
                    streamUrl: `/api/stream/${item.id}`,
                    streamUrlRemux: `/api/stream/${item.id}?remux=1`,
                    vlcUrl: `/api/stream/${item.id}/playlist.m3u`
                  };

                  return (
                    <div
                      key={item.id}
                      onClick={() => onPlay(mediaItem)}
                      className="group flex flex-col rounded-xl overflow-hidden bg-neutral-900 border border-neutral-800 hover:border-neutral-600 cursor-pointer transition-all hover:scale-102"
                    >
                      <div className="relative aspect-video w-full bg-neutral-950">
                        <img
                          src={`/api/thumbnail/${item.id}`}
                          alt={item.name}
                          onError={(e) => { e.target.style.display = 'none'; }}
                          className="w-full h-full object-cover group-hover:brightness-110 transition-all"
                        />
                        <div className="absolute inset-0 bg-black/30 group-hover:opacity-0 transition-opacity" />
                        <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                          <div className="w-10 h-10 rounded-full bg-brand-red text-white flex items-center justify-center shadow-lg">
                            <Play className="w-5 h-5 fill-white ml-0.5" />
                          </div>
                        </div>
                      </div>
                      <div className="p-3">
                        <p className="text-xs font-semibold text-white truncate group-hover:text-brand-red transition-colors">
                          {item.name}
                        </p>
                      </div>
                    </div>
                  );
                }

                return null;
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
