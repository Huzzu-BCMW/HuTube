import React from 'react';
import { Play, Clock, X } from 'lucide-react';
import { clearHistoryItem } from '../utils/storage';

export default function ContinueWatching({ history = [], onPlay, onUpdateHistory }) {
  if (!history || history.length === 0) return null;

  const handleRemove = (e, id) => {
    e.stopPropagation();
    clearHistoryItem(id);
    if (onUpdateHistory) onUpdateHistory();
  };

  const formatRemaining = (current, duration) => {
    if (!duration || duration <= 0) return '';
    const rem = Math.max(0, duration - current);
    const m = Math.ceil(rem / 60);
    return `${m}m left`;
  };

  return (
    <section className="my-6 px-4 md:px-10">
      <div className="flex items-center gap-2 mb-3">
        <Clock className="w-5 h-5 text-brand-red" />
        <h2 className="text-lg md:text-xl font-bold tracking-tight text-white">Continue Watching</h2>
      </div>

      <div className="flex items-center gap-4 overflow-x-auto no-scrollbar py-2">
        {history.map((item) => (
          <div
            key={item.id}
            onClick={() => onPlay(item)}
            className="group relative flex-none w-52 md:w-64 rounded-xl overflow-hidden cursor-pointer bg-neutral-900 border border-neutral-800 hover:border-neutral-600 transition-all duration-300 hover:scale-105 select-none"
          >
            {/* Thumbnail */}
            <div className="relative aspect-video w-full bg-neutral-950">
              <img
                src={`/api/thumbnail/${item.id}`}
                alt={item.title}
                onError={(e) => { e.target.style.display = 'none'; }}
                className="w-full h-full object-cover group-hover:brightness-110 transition-all"
              />

              {/* Dismiss Button */}
              <button
                onClick={(e) => handleRemove(e, item.id)}
                title="Remove from history"
                className="absolute top-2 right-2 p-1 rounded-full bg-black/70 hover:bg-red-600 text-white opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <X className="w-3.5 h-3.5" />
              </button>

              {/* Play Overlay */}
              <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <div className="w-10 h-10 rounded-full bg-brand-red text-white flex items-center justify-center shadow-lg">
                  <Play className="w-5 h-5 fill-white ml-0.5" />
                </div>
              </div>

              {/* Progress Bar */}
              <div className="absolute bottom-0 left-0 right-0 h-1.5 bg-neutral-800">
                <div
                  className="h-full bg-brand-red"
                  style={{ width: `${item.percent || 0}%` }}
                />
              </div>
            </div>

            {/* Info */}
            <div className="p-3">
              <h3 className="text-xs md:text-sm font-semibold text-white truncate" title={item.title}>
                {item.title}
              </h3>
              <div className="flex items-center justify-between text-[11px] text-neutral-400 mt-1">
                <span>{item.seriesTitle ? `${item.seriesTitle} · Ep ${item.episode}` : 'Resume'}</span>
                <span className="text-brand-red font-medium">{formatRemaining(item.currentTime, item.duration)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
