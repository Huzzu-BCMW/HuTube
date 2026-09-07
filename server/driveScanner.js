const driveClient = require('./driveClient');
const axios = require('axios');
const authManager = require('./auth');

const TARGET_CATEGORIES = [
  { id: 'anime', name: 'Anime', icon: '🎌', patterns: [/anime/i] },
  { id: 'cartoon', name: 'Cartoon', icon: '🎨', patterns: [/cartoon/i, /animation/i] },
  { id: 'series', name: 'Series', icon: '📺', patterns: [/series/i, /tv shows?/i, /shows?/i] },
  { id: 'movies', name: 'Movies', icon: '🎬', patterns: [/movies?/i, /films?/i] },
  { id: 'news', name: 'Funny Breaking News', icon: '📰', patterns: [/funny breaking news/i, /breaking news/i, /news/i] }
];

const VIDEO_EXTENSIONS = ['.mp4', '.mkv', '.webm', '.avi', '.mov', '.flv', '.m4v', '.ts', '.wmv'];

function isVideoFile(file) {
  if (file.mimeType && file.mimeType.startsWith('video/')) return true;
  const name = (file.name || '').toLowerCase();
  return VIDEO_EXTENSIONS.some(ext => name.endsWith(ext));
}

function cleanTitle(filename) {
  let name = filename.replace(/\.[^/.]+$/, ''); // remove extension
  name = name.replace(/[._]/g, ' '); // replace dots and underscores with spaces
  return name.trim();
}

function parseEpisodeInfo(filename) {
  const name = filename.toLowerCase();
  
  // Matches S01E02, s1e2, 1x02
  const sMatch = name.match(/s(\d+)\s*e(\d+)/i) || name.match(/(\d+)x(\d+)/i);
  if (sMatch) {
    return {
      season: parseInt(sMatch[1], 10),
      episode: parseInt(sMatch[2], 10)
    };
  }

  // Matches "Episode 12", "Ep 12", "E12"
  const epMatch = name.match(/(?:episode|ep|e)[.\s_-]*(\d+)/i);
  if (epMatch) {
    return {
      season: 1,
      episode: parseInt(epMatch[1], 10)
    };
  }

  // Matches leading digits e.g. "01 - Pilot"
  const leadMatch = name.match(/^(\d{1,3})\b/);
  if (leadMatch) {
    return {
      season: 1,
      episode: parseInt(leadMatch[1], 10)
    };
  }

  return { season: 1, episode: null };
}

function formatDuration(ms) {
  if (!ms) return null;
  const seconds = Math.floor(ms / 1000);
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m ${s}s`;
}

function formatResolution(videoMeta) {
  if (!videoMeta) return null;
  const { width, height } = videoMeta;
  if (height >= 2160 || width >= 3840) return '4K UHD';
  if (height >= 1080 || width >= 1920) return '1080p FHD';
  if (height >= 720 || width >= 1280) return '720p HD';
  if (height) return `${height}p`;
  return null;
}

class DriveScanner {
  constructor() {
    this.catalog = {
      anime: [],
      cartoon: [],
      series: [],
      movies: [],
      news: [],
      otherFolders: [],
      allVideos: [],
      totalCount: 0,
      lastScanned: null,
      scanning: false
    };
  }

  async scanAll() {
    if (this.catalog.scanning) {
      return this.catalog;
    }

    this.catalog.scanning = true;
    console.log('[Scanner] Starting catalog scan across Google Drive...');

    try {
      // 1. Fetch top-level folders
      const rootFoldersRes = await driveClient.listFiles({
        q: "mimeType = 'application/vnd.google-apps.folder'",
        pageSize: 100
      });

      const folders = rootFoldersRes.files || [];
      console.log(`[Scanner] Discovered ${folders.length} folders in Drive root/shared`);

      const categorizedFolders = {
        anime: [],
        cartoon: [],
        series: [],
        movies: [],
        news: [],
        other: []
      };

      for (const folder of folders) {
        let matched = false;
        for (const cat of TARGET_CATEGORIES) {
          if (cat.patterns.some(p => p.test(folder.name))) {
            categorizedFolders[cat.id].push(folder);
            matched = true;
            break;
          }
        }
        if (!matched) {
          categorizedFolders.other.push(folder);
        }
      }

      const allVideos = [];

      // Scan each category
      const scanCategory = async (catId, type) => {
        const catFolders = categorizedFolders[catId];
        const results = [];

        for (const folder of catFolders) {
          const items = await this.scanCategoryFolder(folder, type);
          results.push(...items);
        }

        return results;
      };

      const [movies, series, anime, cartoon, news] = await Promise.all([
        scanCategory('movies', 'movie'),
        scanCategory('series', 'series'),
        scanCategory('anime', 'anime'),
        scanCategory('cartoon', 'cartoon'),
        scanCategory('news', 'clip')
      ]);

      // Collect flat list of all videos for search
      const collectVideos = (items) => {
        for (const item of items) {
          if (item.type === 'video' || item.type === 'movie' || item.type === 'clip') {
            allVideos.push(item);
          } else if (item.episodes) {
            allVideos.push(...item.episodes);
          } else if (item.seasons) {
            for (const s of item.seasons) {
              allVideos.push(...s.episodes);
            }
          }
        }
      };

      collectVideos(movies);
      collectVideos(series);
      collectVideos(anime);
      collectVideos(cartoon);
      collectVideos(news);

      this.catalog = {
        anime,
        cartoon,
        series,
        movies,
        news,
        otherFolders: categorizedFolders.other.map(f => ({ id: f.id, name: f.name })),
        allVideos,
        totalCount: allVideos.length,
        lastScanned: new Date().toISOString(),
        scanning: false
      };

      console.log(`[Scanner] Scan complete! Total catalog videos: ${allVideos.length}`);
      return this.catalog;

    } catch (err) {
      this.catalog.scanning = false;
      console.error('[Scanner] Scan failed:', err.message);
      throw err;
    }
  }

  async scanCategoryFolder(folder, defaultType) {
    const children = await driveClient.listFolderChildren(folder.id);
    const subFolders = children.filter(c => c.mimeType === 'application/vnd.google-apps.folder');
    const videoFiles = children.filter(isVideoFile);

    const items = [];

    // Case 1: Video files directly in category folder (common for Movies, News, or single-season anime)
    for (const file of videoFiles) {
      items.push(this.formatVideoItem(file, folder.name, defaultType));
    }

    // Case 2: Subfolders (e.g. Shows, Movie folders, Season folders)
    for (const sub of subFolders) {
      const subChildren = await driveClient.listFolderChildren(sub.id);
      const subSubFolders = subChildren.filter(c => c.mimeType === 'application/vnd.google-apps.folder');
      const subVideos = subChildren.filter(isVideoFile);

      if (defaultType === 'movie') {
        // If it's in Movies folder, each subfolder is likely a movie title
        if (subVideos.length > 0) {
          items.push(this.formatVideoItem(subVideos[0], sub.name, 'movie', sub.id));
        }
      } else {
        // For Series / Anime / Cartoon:
        // Subfolder is a Show (e.g., "Attack on Titan", "Breaking Bad")
        const showItem = {
          id: sub.id,
          name: sub.name,
          title: cleanTitle(sub.name),
          category: defaultType,
          type: 'show',
          folderId: sub.id,
          thumbnailLink: subVideos[0]?.thumbnailLink || null,
          seasons: [],
          episodes: []
        };

        if (subSubFolders.length > 0) {
          // Has Season folders
          for (const sFolder of subSubFolders) {
            const seasonFiles = await driveClient.listFolderChildren(sFolder.id);
            const seasonVideos = seasonFiles.filter(isVideoFile).map(f =>
              this.formatVideoItem(f, sub.name, defaultType, sFolder.id)
            );
            // Sort episodes naturally
            seasonVideos.sort((a, b) => (a.episode || 0) - (b.episode || 0) || a.name.localeCompare(b.name, undefined, { numeric: true }));

            showItem.seasons.push({
              id: sFolder.id,
              name: sFolder.name,
              episodes: seasonVideos
            });
          }
        } else {
          // Direct episodes in the show folder
          const episodes = subVideos.map(f =>
            this.formatVideoItem(f, sub.name, defaultType, sub.id)
          );
          episodes.sort((a, b) => (a.episode || 0) - (b.episode || 0) || a.name.localeCompare(b.name, undefined, { numeric: true }));
          showItem.episodes = episodes;
        }

        items.push(showItem);
      }
    }

    return items;
  }

  formatVideoItem(file, parentName, category, folderId = null) {
    const { season, episode } = parseEpisodeInfo(file.name);
    const durationFormatted = file.videoMediaMetadata?.durationMillis
      ? formatDuration(file.videoMediaMetadata.durationMillis)
      : null;
    const resolution = formatResolution(file.videoMediaMetadata);

    return {
      id: file.id,
      name: file.name,
      title: cleanTitle(file.name),
      seriesTitle: parentName ? cleanTitle(parentName) : null,
      category,
      type: category === 'movie' ? 'movie' : (category === 'clip' ? 'clip' : 'video'),
      folderId,
      size: file.size ? parseInt(file.size, 10) : 0,
      thumbnailLink: file.thumbnailLink,
      duration: durationFormatted,
      durationMillis: file.videoMediaMetadata?.durationMillis || null,
      resolution,
      season,
      episode,
      streamUrl: `/api/stream/${file.id}`,
      streamUrlRemux: `/api/stream/${file.id}?remux=1`,
      vlcUrl: `/api/stream/${file.id}/playlist.m3u`
    };
  }

  getCatalog() {
    return this.catalog;
  }
}

module.exports = new DriveScanner();
