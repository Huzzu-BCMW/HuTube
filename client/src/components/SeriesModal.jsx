import React, { useState } from 'react';
import { X, Play, Tv, Film, Download, ExternalLink } from 'lucide-react';
import { getProgress } from '../utils/storage';

export default function SeriesModal({ show, onClose, onPlayEpisode }) {
  if (!show) return null;

  const hasSeasons = show.seasons && show.seasons.length > 0;
  const [selectedSeasonIdx, setSelectedSeasonIdx] = useState(0);

  const episodes = hasSeasons
    ? show.seasons[selectedSeasonIdx]?.episodes || []
    : show.episodes || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 md:p-6 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        className="relative w-full max-w-4xl max-h-[90vh] bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Bar with Backdrop */}
        <div className="relative p-6 md:p-8 bg-gradient-to-b from-neutral-800 to-neutral-900 border-b border-neutral-800">
          <button
            onClick={onClose}
            className="absolute top-4 right-4 p-2 rounded-full bg-black/60 hover:bg-neutral-800 text-neutral-300 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
            <div className="w-20 h-28 md:w-28 md:h-40 rounded-xl overflow-hidden bg-neutral-950 flex-none border border-neutral-700 shadow-xl">
              {show.thumbnailLink ? (
                <img
                  src={show.id ? `/api/thumbnail/${show.id}` : show.thumbnailLink}
                  alt={show.title}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-neutral-800 text-neutral-500">
                  <Tv className="w-10 h-10" />
                </div>
              )}
            </div>

            <div className="space-y-2 flex-1">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand-red text-white uppercase tracking-wider">
                {show.category}
              </span>
              <h2 className="text-2xl md:text-3xl font-black text-white">{show.title}</h2>
              <p className="text-xs md:text-sm text-neutral-400">
                {hasSeasons ? `${show.seasons.length} Seasons` : `${episodes.length} Episodes available`}
              </p>

              {episodes.length > 0 && (
                <button
                  onClick={() => onPlayEpisode(episodes[0], show)}
                  className="inline-flex items-center gap-2 px-5 py-2 mt-2 rounded-lg bg-white hover:bg-neutral-200 text-black font-bold text-sm transition-transform hover:scale-105"
                >
                  <Play className="w-4 h-4 fill-black" />
                  <span>Play First Episode</span>
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Season Selector Tabs */}
        {hasSeasons && show.seasons.length > 1 && (
          <div className="flex items-center gap-2 px-6 py-3 bg-neutral-950/60 border-b border-neutral-800 overflow-x-auto no-scrollbar">
            {show.seasons.map((season, idx) => (
              <button
                key={season.id || idx}
                onClick={() => setSelectedSeasonIdx(idx)}
                className={`px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
                  selectedSeasonIdx === idx
                    ? 'bg-brand-red text-white shadow'
                    : 'bg-neutral-800 text-neutral-400 hover:text-white'
                }`}
              >
                {season.name || `Season ${idx + 1}`}
              </button>
            ))}
          </div>
        )}

        {/* Episode List */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-3">
          {episodes.length === 0 ? (
            <div className="text-center py-12 text-neutral-400 text-sm">
              No playable video files found in this folder.
            </div>
          ) : (
            episodes.map((ep, idx) => {
              const savedProgress = getProgress(ep.id);
              const percent = ep.durationMillis && savedProgress > 0
                ? Math.min(100, Math.round((savedProgress / (ep.durationMillis / 1000)) * 100))
                : 0;

              return (
                <div
                  key={ep.id}
                  onClick={() => onPlayEpisode(ep, show)}
                  className="group flex items-center gap-4 p-3 rounded-xl bg-neutral-950/50 hover:bg-neutral-800/80 border border-neutral-800/60 hover:border-neutral-700 cursor-pointer transition-all"
                >
                  {/* Episode Number or Index */}
                  <div className="w-8 text-center text-sm font-bold text-neutral-500 group-hover:text-brand-red">
                    {ep.episode || idx + 1}
                  </div>

                  {/* Thumbnail */}
                  <div className="relative w-28 md:w-36 aspect-video rounded-lg overflow-hidden bg-neutral-900 flex-none">
                    <img
                      src={`/api/thumbnail/${ep.id}`}
                      alt={ep.title}
                      onError={(e) => { e.target.style.display = 'none'; }}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <div className="absolute inset-0 bg-black/30 group-hover:opacity-0 transition-opacity" />
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                      <div className="w-8 h-8 rounded-full bg-brand-red text-white flex items-center justify-center shadow-lg">
                        <Play className="w-4 h-4 fill-white ml-0.5" />
                      </div>
                    </div>

                    {percent > 0 && (
                      <div className="absolute bottom-0 left-0 right-0 h-1 bg-neutral-800">
                        <div className="h-full bg-brand-red" style={{ width: `${percent}%` }} />
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-semibold text-white group-hover:text-brand-red transition-colors truncate">
                      {ep.title}
                    </h4>
                    <div className="flex items-center gap-2 mt-1 text-xs text-neutral-400">
                      {ep.duration && <span>{ep.duration}</span>}
                      {ep.resolution && <span className="px-1.5 py-0.2 rounded bg-neutral-800 text-[10px]">{ep.resolution}</span>}
                    </div>
                  </div>

                  {/* Open in VLC shortcut */}
                  <a
                    href={`/api/stream/${ep.id}/playlist.m3u`}
                    onClick={(e) => e.stopPropagation()}
                    download
                    title="Open / Download VLC M3U Stream"
                    className="p-2 rounded-lg text-neutral-500 hover:text-white hover:bg-neutral-700/60 transition-colors"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
