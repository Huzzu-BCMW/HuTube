const axios = require('axios');
const { spawn } = require('child_process');
const authManager = require('./auth');
const driveClient = require('./driveClient');

// Cache metadata briefly to avoid repeated file lookups
const metaCache = new Map();

async function getCachedMetadata(fileId) {
  if (metaCache.has(fileId)) {
    return metaCache.get(fileId);
  }
  const meta = await driveClient.getFile(fileId);
  metaCache.set(fileId, meta);
  // Expire after 10 minutes
  setTimeout(() => metaCache.delete(fileId), 10 * 60 * 1000);
  return meta;
}

function getContentType(filename, driveMimeType) {
  const ext = (filename || '').split('.').pop().toLowerCase();
  switch (ext) {
    case 'mp4':
    case 'm4v':
      return 'video/mp4';
    case 'mkv':
      return 'video/x-matroska';
    case 'webm':
      return 'video/webm';
    case 'avi':
      return 'video/x-msvideo';
    case 'mov':
      return 'video/quicktime';
    case 'flv':
      return 'video/x-flv';
    case 'ts':
      return 'video/mp2t';
    default:
      return driveMimeType && driveMimeType.startsWith('video/') ? driveMimeType : 'video/mp4';
  }
}

/**
 * Handle direct HTTP Range streaming from Google Drive
 */
async function handleStream(req, res) {
  const { fileId } = req.params;
  const remux = req.query.remux === '1' || req.query.remux === 'true';

  try {
    const token = await authManager.getValidToken();
    if (!token) {
      return res.status(401).json({ error: 'Google Drive is not authenticated' });
    }

    const fileMeta = await getCachedMetadata(fileId);
    const rangeHeader = req.headers.range;
    const gdriveUrl = `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`;
    const contentType = getContentType(fileMeta.name, fileMeta.mimeType);

    // If FFmpeg remux is explicitly requested (e.g. MKV or unsupported browser audio)
    if (remux) {
      return handleRemuxStream(req, res, gdriveUrl, token, fileMeta);
    }

    const requestHeaders = {
      Authorization: `Bearer ${token}`
    };

    if (rangeHeader) {
      requestHeaders.Range = rangeHeader;
    }

    const response = await axios({
      method: 'GET',
      url: gdriveUrl,
      headers: requestHeaders,
      responseType: 'stream',
      validateStatus: (status) => status >= 200 && status < 400
    });

    const isPartial = response.status === 206;
    const status = isPartial ? 206 : 200;

    const responseHeaders = {
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes',
      'Content-Disposition': `inline; filename="${encodeURIComponent(fileMeta.name)}"`,
      'Cache-Control': 'no-cache'
    };

    if (response.headers['content-range']) {
      responseHeaders['Content-Range'] = response.headers['content-range'];
    }
    if (response.headers['content-length']) {
      responseHeaders['Content-Length'] = response.headers['content-length'];
    } else if (fileMeta.size && !isPartial) {
      responseHeaders['Content-Length'] = fileMeta.size;
    }

    res.writeHead(status, responseHeaders);
    response.data.pipe(res);

    // Handle client disconnect to stop upstream Google Drive stream
    req.on('close', () => {
      if (response.data && !response.data.destroyed) {
        response.data.destroy();
      }
    });

  } catch (error) {
    console.error(`[Streamer] Error streaming file ${fileId}:`, error.message);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Stream failed', message: error.message });
    }
  }
}

/**
 * Remux stream on-the-fly via FFmpeg for codecs incompatible with standard browsers
 */
async function handleRemuxStream(req, res, gdriveUrl, token, fileMeta) {
  console.log(`[Streamer] Starting FFmpeg live remux for: ${fileMeta.name}`);

  const startSeconds = parseFloat(req.query.startTime || 0);

  const ffmpegArgs = [
    '-headers', `Authorization: Bearer ${token}\r\n`,
    ...(startSeconds > 0 ? ['-ss', startSeconds.toString()] : []),
    '-i', gdriveUrl,
    '-c:v', 'copy',           // zero re-encoding for video stream (instant speed)
    '-c:a', 'aac',            // convert audio to AAC stereo for universal browser support
    '-b:a', '192k',
    '-movflags', 'frag_keyframe+empty_moov+default_base_moof',
    '-f', 'mp4',
    'pipe:1'
  ];

  const ffmpeg = spawn('ffmpeg', ffmpegArgs);

  res.writeHead(200, {
    'Content-Type': 'video/mp4',
    'Accept-Ranges': 'none',
    'Content-Disposition': `inline; filename="${encodeURIComponent(fileMeta.name)}.mp4"`,
    'Cache-Control': 'no-cache'
  });

  ffmpeg.stdout.pipe(res);

  ffmpeg.stderr.on('data', (data) => {
    // Uncomment for verbose ffmpeg logging
    // console.log(`[FFmpeg] ${data.toString().trim()}`);
  });

  ffmpeg.on('error', (err) => {
    console.error('[FFmpeg] Error:', err.message);
  });

  req.on('close', () => {
    try {
      ffmpeg.kill('SIGKILL');
    } catch (_) {}
  });
}

/**
 * Generates an M3U playlist file to open directly in VLC, PotPlayer, MPV, etc.
 */
async function handleM3uPlaylist(req, res) {
  const { fileId } = req.params;
  try {
    const fileMeta = await getCachedMetadata(fileId);
    const host = req.get('host');
    const protocol = req.protocol;
    const streamUrl = `${protocol}://${host}/api/stream/${fileId}`;

    const m3uContent = `#EXTM3U\n#EXTINF:-1,${fileMeta.name}\n${streamUrl}\n`;

    res.setHeader('Content-Type', 'audio/x-mpegurl');
    res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(fileMeta.name)}.m3u"`);
    res.send(m3uContent);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
}

module.exports = {
  handleStream,
  handleM3uPlaylist,
  getCachedMetadata
};
