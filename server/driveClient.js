const { google } = require('googleapis');
const authManager = require('./auth');

class DriveClient {
  getDrive() {
    if (!authManager.oauth2Client || !authManager.tokens) {
      throw new Error('Google Drive is not authenticated. Please log in.');
    }
    return google.drive({ version: 'v3', auth: authManager.oauth2Client });
  }

  async getAbout() {
    const drive = this.getDrive();
    const res = await drive.about.get({
      fields: 'user, storageQuota'
    });
    return res.data;
  }

  async listFiles({ q = '', pageSize = 100, pageToken = null, fields = 'nextPageToken, files(id, name, mimeType, size, createdTime, modifiedTime, thumbnailLink, hasThumbnail, videoMediaMetadata, parents)' }) {
    const drive = this.getDrive();
    const res = await drive.files.list({
      q: q ? `trashed = false and (${q})` : 'trashed = false',
      pageSize,
      pageToken,
      fields,
      orderBy: 'name natural',
      supportsAllDrives: true,
      includeItemsFromAllDrives: true
    });
    return res.data;
  }

  async getFile(fileId) {
    const drive = this.getDrive();
    const res = await drive.files.get({
      fileId,
      fields: 'id, name, mimeType, size, createdTime, modifiedTime, thumbnailLink, hasThumbnail, videoMediaMetadata, parents',
      supportsAllDrives: true
    });
    return res.data;
  }

  async searchFolder(name, parentId = null) {
    let q = `mimeType = 'application/vnd.google-apps.folder' and name = '${name.replace(/'/g, "\\'")}'`;
    if (parentId) {
      q += ` and '${parentId}' in parents`;
    }
    const result = await this.listFiles({ q, pageSize: 10 });
    return result.files && result.files.length > 0 ? result.files[0] : null;
  }

  async listFolderChildren(folderId) {
    const q = `'${folderId}' in parents`;
    let allFiles = [];
    let pageToken = null;

    do {
      const res = await this.listFiles({ q, pageSize: 100, pageToken });
      if (res.files) {
        allFiles = allFiles.concat(res.files);
      }
      pageToken = res.nextPageToken;
    } while (pageToken);

    return allFiles;
  }
}

module.exports = new DriveClient();
