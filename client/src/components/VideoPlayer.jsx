import React, { useState, useEffect, useRef } from 'react';
import {
  Play, Pause, RotateCcw, RotateCw, Volume2, VolumeX,
  Maximize, Minimize, SkipForward, ArrowLeft, Settings,
  ExternalLink, Check, Copy, FastForward
} from 'lucide-react';
import { saveProgress, getProgress } from '../utils/storage';

export default function VideoPlayer({
  item,
  seriesContext = null,
  onClose,
  onPlayNext
}) {
  const videoRef = useRef(null);
  const containerRef = useRef(null);
  const controlsTimeoutRef = useRef(null);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [showSettings, setShowSettings] = useState(false);
  const [useRemux, setUseRemux] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [videoError, setVideoError] = useState(null);
  const [nextCountdown, setNextCountdown] = useState(null);

  // Determine current stream URL
  const streamUrl = useRemux ? item.streamUrlRemux : item.streamUrl;

  // Next episode resolution
  const getNextEpisode = () => {
    if (!seriesContext) return null;
    const episodes = seriesContext.episodes || 
      seriesContext.seasons?.flatMap(s => s.episodes) || [];
    const currentIdx = episodes.findIndex(e => e.id === item.id);
    if (currentIdx !== -1 && currentIdx + 1 < episodes.length) {
      return episodes[currentIdx + 1];
    }
    return null;
  };

  const nextEpisode = getNextEpisode();

  // Reset states on item change & restore progress
  useEffect(() => {
    setVideoError(null);
    setNextCountdown(null);
    if (videoRef.current) {
      videoRef.current.playbackRate = playbackRate;
      const initialProgress = getProgress(item.id);
      if (initialProgress > 10) {
        videoRef.current.currentTime = initialProgress;
      }
      videoRef.current.play().catch(() => setIsPlaying(false));
    }
  }, [item.id, useRemux]);

  // Periodic progress saving
  useEffect(() => {
    const interval = setInterval(() => {
      if (videoRef.current && !videoRef.current.paused && videoRef.current.duration) {
        saveProgress(item, videoRef.current.currentTime, videoRef.current.duration);
      }
    }, 4000);
    return () => clearInterval(interval);
  }, [item]);

  // Handle controls visibility with mouse movement
  const handleMouseMove = () => {
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => {
      if (isPlaying) {
        setShowControls(false);
        setShowSettings(false);
      }
    }, 3000);
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (['input', 'textarea'].includes(e.target.tagName.toLowerCase())) return;

      switch (e.key) {
        case ' ':
        case 'k':
          e.preventDefault();
          togglePlay();
          break;
        case 'ArrowLeft':
        case 'j':
          e.preventDefault();
          seekBy(-10);
          break;
        case 'ArrowRight':
        case 'l':
          e.preventDefault();
          seekBy(10);
          break;
        case 'ArrowUp':
          e.preventDefault();
          setVolume(v => {
            const nv = Math.min(1, v + 0.1);
            if (videoRef.current) videoRef.current.volume = nv;
            return nv;
          });
          break;
        case 'ArrowDown':
          e.preventDefault();
          setVolume(v => {
            const nv = Math.max(0, v - 0.1);
            if (videoRef.current) videoRef.current.volume = nv;
            return nv;
          });
          break;
        case 'f':
        case 'F':
          e.preventDefault();
          toggleFullscreen();
          break;
        case 'm':
        case 'M':
          e.preventDefault();
          toggleMute();
          break;
        case 'Escape':
          if (isFullscreen) {
            document.exitFullscreen().catch(() => {});
          } else {
            onClose();
          }
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isPlaying, isFullscreen]);

  const togglePlay = () => {
    if (!videoRef.current) return;
    if (videoRef.current.paused) {
      videoRef.current.play();
      setIsPlaying(true);
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
    }
  };

  const seekBy = (seconds) => {
    if (!videoRef.current) return;
    videoRef.current.currentTime = Math.max(0, Math.min(videoRef.current.duration || 0, videoRef.current.currentTime + seconds));
  };

  const handleSeek = (e) => {
    if (!videoRef.current) return;
    const seekTo = (parseFloat(e.target.value) / 100) * duration;
    videoRef.current.currentTime = seekTo;
    setCurrentTime(seekTo);
  };

  const toggleMute = () => {
    if (!videoRef.current) return;
    videoRef.current.muted = !isMuted;
    setIsMuted(!isMuted);
  };

  const handleVolumeChange = (e) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
    if (videoRef.current) {
      videoRef.current.volume = val;
      videoRef.current.muted = val === 0;
      setIsMuted(val === 0);
    }
  };

  const toggleFullscreen = () => {
    if (!containerRef.current) return;
    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen().catch(err => console.error(err));
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  const handlePlaybackRate = (rate) => {
    setPlaybackRate(rate);
    if (videoRef.current) videoRef.current.playbackRate = rate;
  };

  const handleVideoEnded = () => {
    if (nextEpisode) {
      let count = 5;
      setNextCountdown(count);
      const timer = setInterval(() => {
        count -= 1;
        setNextCountdown(count);
        if (count <= 0) {
          clearInterval(timer);
          onPlayNext(nextEpisode);
        }
      }, 1000);
    }
  };

  const handleCopyLink = () => {
    const absoluteUrl = `${window.location.origin}/api/stream/${item.id}`;
    navigator.clipboard.writeText(absoluteUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '00:00';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    const pad = (n) => n.toString().padStart(2, '0');
    if (h > 0) return `${h}:${pad(m)}:${pad(s)}`;
    return `${pad(m)}:${pad(s)}`;
  };

  const progressPercent = duration ? (currentTime / duration) * 100 : 0;

  return (
    <div
      ref={containerRef}
      onMouseMove={handleMouseMove}
      className="fixed inset-0 z-50 bg-black flex items-center justify-center select-none overflow-hidden"
    >
      {/* HTML5 Video Element */}
      <video
        ref={videoRef}
        src={streamUrl}
        className="w-full h-full object-contain"
        playsInline
        onTimeUpdate={() => setCurrentTime(videoRef.current?.currentTime || 0)}
        onDurationChange={() => setDuration(videoRef.current?.duration || 0)}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={handleVideoEnded}
        onError={() => setVideoError('Playback error. The video container or audio codec might not be natively supported by your browser.')}
        onClick={togglePlay}
      />

      {/* Video Error Banner / Recovery */}
      {videoError && (
        <div className="absolute inset-0 bg-black/90 flex flex-col items-center justify-center p-6 text-center z-40 max-w-lg mx-auto">
          <div className="w-14 h-14 rounded-full bg-red-600/20 text-red-500 flex items-center justify-center mb-4">
            <ExternalLink className="w-7 h-7" />
          </div>
          <h3 className="text-xl font-bold text-white mb-2">Unsupported Codec or Container</h3>
          <p className="text-sm text-neutral-300 mb-6">
            This file might use MKV container or audio formats (like AC3/EAC3) that Google Drive and web browsers can't decode directly.
          </p>

          <div className="flex flex-col sm:flex-row items-center gap-3 w-full">
            <button
              onClick={() => {
                setUseRemux(true);
                setVideoError(null);
              }}
              className="w-full py-2.5 px-4 rounded-lg bg-brand-red hover:bg-red-700 text-white font-bold text-sm transition-all"
            >
              🚀 Play with Live FFmpeg Remux
            </button>

            <a
              href={`/api/stream/${item.id}/playlist.m3u`}
              download
              className="w-full py-2.5 px-4 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-white font-bold text-sm transition-all text-center"
            >
              🍿 Open in VLC / External Player
            </a>
          </div>
        </div>
      )}

      {/* Next Episode Auto-play Overlay */}
      {nextCountdown !== null && nextEpisode && (
        <div className="absolute bottom-24 right-8 z-40 bg-neutral-900/90 border border-neutral-700 p-5 rounded-2xl shadow-2xl backdrop-blur-md max-w-sm animate-in slide-in-from-bottom-5">
          <p className="text-xs uppercase font-bold text-neutral-400">Up Next in {nextCountdown}s</p>
          <h4 className="text-base font-bold text-white mt-1 truncate">{nextEpisode.title}</h4>
          <div className="flex items-center gap-3 mt-4">
            <button
              onClick={() => onPlayNext(nextEpisode)}
              className="flex items-center gap-1.5 px-4 py-2 bg-white hover:bg-neutral-200 text-black text-xs font-bold rounded-lg transition-all"
            >
              <Play className="w-4 h-4 fill-black" /> Play Now
            </button>
            <button
              onClick={() => setNextCountdown(null)}
              className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-semibold rounded-lg transition-all"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Top Header Bar (Title, Back, VLC) */}
      <div
        className={`absolute top-0 left-0 right-0 p-4 md:p-6 bg-gradient-to-b from-black/80 via-black/40 to-transparent flex items-center justify-between transition-opacity duration-300 z-30 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        <div className="flex items-center gap-4 min-w-0">
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-black/50 hover:bg-neutral-800 text-white transition-colors"
          >
            <ArrowLeft className="w-6 h-6" />
          </button>
          <div className="min-w-0">
            <h2 className="text-base md:text-lg font-bold text-white truncate">{item.title}</h2>
            {item.seriesTitle && (
              <p className="text-xs text-neutral-400 truncate">
                {item.seriesTitle} {item.episode ? `· Episode ${item.episode}` : ''}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Skip Intro Button */}
          <button
            onClick={() => seekBy(85)}
            className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/15 hover:bg-white/25 text-xs font-bold text-white border border-white/20 transition-all backdrop-blur-md"
            title="Skip Intro (+85s)"
          >
            <FastForward className="w-4 h-4" />
            <span>Skip Intro</span>
          </button>

          {/* Copy Stream Link */}
          <button
            onClick={handleCopyLink}
            className="p-2 rounded-lg bg-black/50 hover:bg-neutral-800 text-neutral-300 hover:text-white transition-colors"
            title="Copy Direct Stream Link"
          >
            {copiedLink ? <Check className="w-5 h-5 text-green-400" /> : <Copy className="w-5 h-5" />}
          </button>

          {/* Download VLC Playlist */}
          <a
            href={`/api/stream/${item.id}/playlist.m3u`}
            download
            className="p-2 rounded-lg bg-black/50 hover:bg-neutral-800 text-neutral-300 hover:text-white transition-colors"
            title="Open in VLC (Download M3U)"
          >
            <ExternalLink className="w-5 h-5" />
          </a>
        </div>
      </div>

      {/* Bottom Controls Bar */}
      <div
        className={`absolute bottom-0 left-0 right-0 px-4 md:px-8 pb-6 pt-16 bg-gradient-to-t from-black/90 via-black/50 to-transparent transition-opacity duration-300 z-30 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        {/* Scrubber Bar */}
        <div className="relative group/scrubber py-2 cursor-pointer mb-2">
          <input
            type="range"
            min="0"
            max="100"
            step="0.1"
            value={progressPercent || 0}
            onChange={handleSeek}
            className="w-full h-1.5 group-hover/scrubber:h-2.5 bg-neutral-700/80 rounded-lg appearance-none cursor-pointer accent-brand-red transition-all"
          />
        </div>

        {/* Buttons Row */}
        <div className="flex items-center justify-between">
          {/* Left Controls */}
          <div className="flex items-center gap-3 md:gap-5">
            {/* Play/Pause */}
            <button
              onClick={togglePlay}
              className="p-2 text-white hover:text-brand-red transition-colors"
            >
              {isPlaying ? <Pause className="w-6 h-6 fill-white" /> : <Play className="w-6 h-6 fill-white" />}
            </button>

            {/* Seek Back 10s */}
            <button
              onClick={() => seekBy(-10)}
              className="p-2 text-neutral-300 hover:text-white transition-colors"
              title="Seek back 10s"
            >
              <RotateCcw className="w-5 h-5" />
            </button>

            {/* Seek Forward 10s */}
            <button
              onClick={() => seekBy(10)}
              className="p-2 text-neutral-300 hover:text-white transition-colors"
              title="Seek forward 10s"
            >
              <RotateCw className="w-5 h-5" />
            </button>

            {/* Next Episode Button */}
            {nextEpisode && (
              <button
                onClick={() => onPlayNext(nextEpisode)}
                className="p-2 text-neutral-300 hover:text-white transition-colors"
                title={`Next: ${nextEpisode.title}`}
              >
                <SkipForward className="w-5 h-5" />
              </button>
            )}

            {/* Volume Control */}
            <div className="flex items-center gap-2 group/volume">
              <button onClick={toggleMute} className="p-2 text-neutral-300 hover:text-white">
                {isMuted || volume === 0 ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
              </button>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={isMuted ? 0 : volume}
                onChange={handleVolumeChange}
                className="w-16 md:w-24 h-1 bg-neutral-700 rounded-lg appearance-none cursor-pointer accent-brand-red"
              />
            </div>

            {/* Time Stamp */}
            <span className="text-xs md:text-sm text-neutral-400 font-medium">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          {/* Right Controls */}
          <div className="flex items-center gap-3 relative">
            {/* Settings Button */}
            <div className="relative">
              <button
                onClick={() => setShowSettings(!showSettings)}
                className={`p-2 rounded-lg transition-colors ${
                  showSettings ? 'text-brand-red bg-white/10' : 'text-neutral-300 hover:text-white'
                }`}
              >
                <Settings className="w-5 h-5" />
              </button>

              {/* Settings Dropdown */}
              {showSettings && (
                <div className="absolute right-0 bottom-12 w-56 glass-dropdown rounded-xl p-3 shadow-2xl space-y-3 z-50 text-xs">
                  <div>
                    <span className="font-bold text-neutral-300 block mb-1">Playback Speed</span>
                    <div className="grid grid-cols-3 gap-1">
                      {[0.5, 0.75, 1, 1.25, 1.5, 2].map(rate => (
                        <button
                          key={rate}
                          onClick={() => handlePlaybackRate(rate)}
                          className={`py-1 rounded text-center font-semibold ${
                            playbackRate === rate ? 'bg-brand-red text-white' : 'bg-neutral-800 text-neutral-400 hover:text-white'
                          }`}
                        >
                          {rate}x
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="border-t border-neutral-800 pt-2">
                    <label className="flex items-center justify-between cursor-pointer py-1">
                      <span className="text-neutral-300">Live FFmpeg Remux</span>
                      <input
                        type="checkbox"
                        checked={useRemux}
                        onChange={(e) => setUseRemux(e.target.checked)}
                        className="accent-brand-red cursor-pointer"
                      />
                    </label>
                    <p className="text-[10px] text-neutral-500 mt-0.5">Use if audio has no sound or container fails.</p>
                  </div>
                </div>
              )}
            </div>

            {/* Fullscreen Button */}
            <button
              onClick={toggleFullscreen}
              className="p-2 text-neutral-300 hover:text-white transition-colors"
            >
              {isFullscreen ? <Minimize className="w-5 h-5" /> : <Maximize className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
