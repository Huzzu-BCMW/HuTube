import React from 'react';
import { Play, Info, Sparkles, Tv, Film } from 'lucide-react';

export default function HeroBanner({ featuredItem, onPlay, onOpenShow }) {
  if (!featuredItem) return null;

  const isShow = featuredItem.type === 'show';
  const backdropUrl = featuredItem.id ? `/api/thumbnail/${featuredItem.id}` : featuredItem.thumbnailLink;

  const handleAction = () => {
    if (isShow) {
      onOpenShow(featuredItem);
    } else {
      onPlay(featuredItem);
    }
  };

  return (
    <div className="relative w-full h-[52vh] md:h-[68vh] min-h-[380px] max-h-[640px] flex items-end pb-12 px-4 md:px-12 overflow-hidden select-none">
      {/* Background Image / Backdrop */}
      {backdropUrl && (
        <div className="absolute inset-0 z-0">
          <img
            src={backdropUrl}
            alt={featuredItem.title}
            className="w-full h-full object-cover object-center filter brightness-60 scale-105 transition-transform duration-1000"
          />
        </div>
      )}

      {/* Cinematic Vignette & Gradients */}
      <div className="absolute inset-0 bg-gradient-to-t from-[#141414] via-[#141414]/60 to-transparent z-10" />
      <div className="absolute inset-0 bg-gradient-to-r from-[#141414]/90 via-[#141414]/40 to-transparent z-10" />

      {/* Featured Content Overlay */}
      <div className="relative z-20 max-w-2xl space-y-3">
        {/* Category & Tag */}
        <div className="flex items-center gap-2.5">
          <span className="flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-brand-red text-white shadow-md">
            <Sparkles className="w-3.5 h-3.5" /> Featured {featuredItem.category}
          </span>
          {featuredItem.resolution && (
            <span className="px-2 py-0.5 rounded text-xs font-semibold bg-white/10 text-neutral-200 border border-white/20">
              {featuredItem.resolution}
            </span>
          )}
          {featuredItem.duration && (
            <span className="text-xs text-neutral-300 font-medium">
              {featuredItem.duration}
            </span>
          )}
        </div>

        {/* Title */}
        <h1 className="text-3xl md:text-5xl lg:text-6xl font-black text-white tracking-tight leading-tight line-clamp-2 drop-shadow-lg">
          {featuredItem.title}
        </h1>

        {/* Subtitle / Series Info */}
        <p className="text-sm md:text-base text-neutral-300 line-clamp-2 leading-relaxed">
          {isShow 
            ? `Explore all seasons and episodes streamed directly from your Google Drive with zero buffer delays.`
            : `Stream this title in crystal clear quality without Google Drive preview quotas or playback limits.`
          }
        </p>

        {/* Action Buttons */}
        <div className="flex items-center gap-3 pt-2">
          <button
            onClick={handleAction}
            className="flex items-center gap-2.5 px-6 py-2.5 rounded-lg bg-white hover:bg-neutral-200 text-black font-bold text-sm md:text-base transition-all transform hover:scale-105 shadow-xl"
          >
            <Play className="w-5 h-5 fill-black" />
            <span>{isShow ? 'Watch Now' : 'Play'}</span>
          </button>

          {isShow && (
            <button
              onClick={() => onOpenShow(featuredItem)}
              className="flex items-center gap-2 px-5 py-2.5 rounded-lg bg-neutral-800/80 hover:bg-neutral-700/80 text-white font-semibold text-sm md:text-base backdrop-blur-md border border-white/10 transition-all"
            >
              <Tv className="w-5 h-5 text-neutral-300" />
              <span>Episodes</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
