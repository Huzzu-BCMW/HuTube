import React, { useState } from 'react';
import { Play, Tv, Film, MoreVertical, ExternalLink } from 'lucide-react';
import { getProgress } from '../utils/storage';

export default function MediaCard({ item, onPlay, onOpenShow }) {
  const [imgError, setImgError] = useState(false);
  const isShow = item.type === 'show';
  const savedTime = !isShow ? getProgress(item.id) : 0;
  const progressPercent = item.durationMillis && savedTime > 0
    ? Math.min(100, Math.round((savedTime / (item.durationMillis / 1000)) * 100))
    : 0;

  // Use proxy thumbnail for reliable cross-origin loading
  const thumbnailSrc = item.id ? `/api/thumbnail/${item.id}` : item.thumbnailLink;

  const handleClick = () => {
    if (isShow) {
      onOpenShow(item);
    } else {
      onPlay(item);
    }
  };

  const getBadge = () => {
    if (isShow) {
      const epCount = item.episodes?.length || 
        item.seasons?.reduce((acc, s) => acc + (s.episodes?.length || 0), 0) || 0;
      const seasonCount = item.seasons?.length;
      if (seasonCount > 1) return `${seasonCount} Seasons`;
      return `${epCount} Episodes`;
    }
    if (item.resolution) return item.resolution;
    if (item.duration) return item.duration;
    return null;
  };

  return (
    <div 
      onClick={handleClick}
      className="group relative flex-none w-44 md:w-56 rounded-xl overflow-hidden cursor-pointer bg-neutral-900 border border-neutral-800/80 hover:border-neutral-600 transition-all duration-300 hover:scale-105 hover:shadow-2xl hover:shadow-black/80 hover:z-20 select-none"
    >
      {/* Thumbnail Aspect Ratio 16:9 */}
      <div className="relative aspect-video w-full bg-neutral-950 overflow-hidden">
        {!imgError && thumbnailSrc ? (
          <img
            src={thumbnailSrc}
            alt={item.title}
            onError={() => setImgError(true)}
            className="w-full h-full object-cover object-center group-hover:brightness-110 transition-all duration-300"
            loading="lazy"
          />
        ) : (
          /* Fallback sleek cinematic gradient */
          <div className="w-full h-full flex flex-col items-center justify-center p-3 text-center bg-gradient-to-br from-neutral-800 via-neutral-900 to-black">
            {isShow ? (
              <Tv className="w-8 h-8 text-neutral-500 mb-1" />
            ) : (
              <Film className="w-8 h-8 text-neutral-500 mb-1" />
            )}
            <span className="text-xs font-semibold text-neutral-300 line-clamp-2">{item.title}</span>
          </div>
        )}

        {/* Category / Quality badge */}
        {getBadge() && (
          <span className="absolute top-2 right-2 px-2 py-0.5 rounded text-[10px] font-bold bg-black/75 backdrop-blur-md text-neutral-200 border border-white/10">
            {getBadge()}
          </span>
        )}

        {/* Season & Episode pill if episode */}
        {item.episode && (
          <span className="absolute top-2 left-2 px-2 py-0.5 rounded text-[10px] font-bold bg-brand-red text-white shadow">
            EP {item.episode}
          </span>
        )}

        {/* Hover Play Button Overlay */}
        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
          <div className="w-12 h-12 rounded-full bg-brand-red text-white flex items-center justify-center shadow-lg shadow-brand-red/40 transform scale-75 group-hover:scale-100 transition-transform">
            <Play className="w-6 h-6 fill-white ml-0.5" />
          </div>
        </div>

        {/* Progress Bar (if partially watched) */}
        {progressPercent > 0 && (
          <div className="absolute bottom-0 left-0 right-0 h-1 bg-neutral-800">
            <div 
              className="h-full bg-brand-red" 
              style={{ width: `${progressPercent}%` }} 
            />
          </div>
        )}
      </div>

      {/* Card Info */}
      <div className="p-3">
        <h3 className="text-xs md:text-sm font-semibold text-white truncate group-hover:text-brand-red transition-colors" title={item.title}>
          {item.title}
        </h3>
        
        <div className="flex items-center justify-between text-[11px] text-neutral-400 mt-1">
          <span className="capitalize">{item.seriesTitle || item.category || 'Video'}</span>
          {item.duration && <span>{item.duration}</span>}
        </div>
      </div>
    </div>
  );
}
