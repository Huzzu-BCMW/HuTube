const WATCH_HISTORY_KEY = 'hutube_watch_history';
const SETTINGS_KEY = 'hutube_settings';

export function getWatchHistory() {
  try {
    const raw = localStorage.getItem(WATCH_HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    return [];
  }
}

export function saveProgress(item, currentTime, duration) {
  if (!item || !item.id || !duration || duration <= 0) return;
  
  const history = getWatchHistory();
  const percent = Math.min(100, Math.round((currentTime / duration) * 100));
  
  // Don't save if finished (e.g. > 95%) or if just started (< 5s)
  const existingIdx = history.findIndex(h => h.id === item.id);
  
  if (percent >= 95) {
    if (existingIdx !== -1) {
      history.splice(existingIdx, 1);
      localStorage.setItem(WATCH_HISTORY_KEY, JSON.stringify(history));
    }
    return;
  }

  const record = {
    id: item.id,
    title: item.title || item.name,
    seriesTitle: item.seriesTitle || null,
    thumbnailLink: item.thumbnailLink || null,
    season: item.season || 1,
    episode: item.episode || null,
    currentTime,
    duration,
    percent,
    streamUrl: item.streamUrl,
    vlcUrl: item.vlcUrl,
    updatedAt: Date.now()
  };

  if (existingIdx !== -1) {
    history[existingIdx] = record;
  } else {
    history.unshift(record);
  }

  // Keep max 20 items
  const trimmed = history.slice(0, 20);
  localStorage.setItem(WATCH_HISTORY_KEY, JSON.stringify(trimmed));
}

export function getProgress(fileId) {
  const history = getWatchHistory();
  const found = history.find(h => h.id === fileId);
  return found ? found.currentTime : 0;
}

export function clearHistoryItem(fileId) {
  const history = getWatchHistory().filter(h => h.id !== fileId);
  localStorage.setItem(WATCH_HISTORY_KEY, JSON.stringify(history));
}
