package com.example.server

object WebClientHtml {

    fun getHtml(): String {
        val d = "$" // dollar sign helper for Kotlin string templates to avoid JS interpolation clash
        return """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>PhotoShare Hub - Local Gallery Explorer</title>
  <style>
    :root {
      --bg-base: #0B1120;
      --bg-surface: #1E293B;
      --bg-surface-hover: #334155;
      --border-color: #334155;
      --text-primary: #F8FAFC;
      --text-secondary: #94A3B8;
      --accent-cyan: #06B6D4;
      --accent-indigo: #6366F1;
      --accent-emerald: #10B981;
      --radius: 16px;
      --radius-sm: 8px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }

    body {
      background-color: var(--bg-base);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    header {
      background-color: var(--bg-surface);
      border-bottom: 1px solid var(--border-color);
      position: sticky;
      top: 0;
      z-index: 50;
      backdrop-filter: blur(12px);
    }

    .header-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 1rem 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .brand-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, var(--accent-cyan), var(--accent-indigo));
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
      font-size: 1.25rem;
      box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
    }

    .brand-text h1 {
      font-size: 1.25rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }

    .brand-text p {
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.6rem 1.2rem;
      border-radius: var(--radius-sm);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
      text-decoration: none;
    }

    .btn-primary {
      background: linear-gradient(135deg, var(--accent-cyan), var(--accent-indigo));
      color: white;
      box-shadow: 0 2px 8px rgba(6, 182, 212, 0.25);
    }

    .btn-primary:hover {
      opacity: 0.92;
      transform: translateY(-1px);
    }

    .btn-secondary {
      background-color: var(--bg-surface-hover);
      color: var(--text-primary);
    }

    .btn-secondary:hover {
      background-color: #475569;
    }

    .main-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 1.5rem;
      flex: 1;
      width: 100%;
    }

    .controls-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 1.5rem;
      background-color: var(--bg-surface);
      padding: 1rem 1.25rem;
      border-radius: var(--radius);
      border: 1px solid var(--border-color);
    }

    .search-box {
      position: relative;
      flex: 1;
      min-width: 260px;
    }

    .search-box input {
      width: 100%;
      background-color: var(--bg-base);
      border: 1px solid var(--border-color);
      padding: 0.65rem 1rem 0.65rem 2.5rem;
      border-radius: var(--radius-sm);
      color: var(--text-primary);
      font-size: 0.9rem;
      outline: none;
      transition: border-color 0.2s;
    }

    .search-box input:focus {
      border-color: var(--accent-cyan);
    }

    .search-icon {
      position: absolute;
      left: 0.85rem;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-secondary);
      pointer-events: none;
    }

    .albums-nav {
      display: flex;
      gap: 0.5rem;
      overflow-x: auto;
      padding-bottom: 0.25rem;
      max-width: 100%;
    }

    .album-chip {
      background-color: var(--bg-base);
      color: var(--text-secondary);
      border: 1px solid var(--border-color);
      padding: 0.45rem 0.9rem;
      border-radius: 999px;
      font-size: 0.825rem;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
    }

    .album-chip:hover {
      color: var(--text-primary);
      border-color: var(--accent-cyan);
    }

    .album-chip.active {
      background-color: var(--accent-cyan);
      color: #000;
      border-color: var(--accent-cyan);
      font-weight: 600;
    }

    .stats-info {
      font-size: 0.85rem;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: var(--accent-emerald);
      display: inline-block;
      box-shadow: 0 0 8px var(--accent-emerald);
    }

    .gallery-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1.25rem;
    }

    .photo-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
      cursor: pointer;
      position: relative;
    }

    .photo-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
      border-color: var(--accent-cyan);
    }

    .thumb-wrapper {
      position: relative;
      width: 100%;
      padding-top: 75%;
      background-color: #000;
      overflow: hidden;
    }

    .thumb-wrapper img {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }

    .photo-card:hover .thumb-wrapper img {
      transform: scale(1.04);
    }

    .album-badge {
      position: absolute;
      bottom: 8px;
      left: 8px;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      color: #fff;
      font-size: 0.7rem;
      padding: 2px 8px;
      border-radius: 4px;
      font-weight: 500;
    }

    .size-badge {
      position: absolute;
      top: 8px;
      right: 8px;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      color: #fff;
      font-size: 0.7rem;
      padding: 2px 8px;
      border-radius: 4px;
      font-weight: 500;
    }

    .card-info {
      padding: 0.9rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .photo-title {
      font-size: 0.875rem;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .photo-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: var(--text-secondary);
    }

    .empty-state {
      text-align: center;
      padding: 4rem 1rem;
      color: var(--text-secondary);
      grid-column: 1 / -1;
    }

    .empty-state svg {
      width: 64px;
      height: 64px;
      margin-bottom: 1rem;
      color: var(--text-secondary);
    }

    /* Lightbox Modal */
    .lightbox {
      display: none;
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.92);
      backdrop-filter: blur(8px);
      z-index: 100;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
    }

    .lightbox.active {
      display: flex;
    }

    .lightbox-content {
      position: relative;
      max-width: 90vw;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .lightbox-img {
      max-width: 100%;
      max-height: 80vh;
      object-fit: contain;
      border-radius: var(--radius-sm);
      box-shadow: 0 10px 40px rgba(0,0,0,0.8);
    }

    .lightbox-bar {
      margin-top: 1rem;
      display: flex;
      align-items: center;
      gap: 1rem;
      background: var(--bg-surface);
      padding: 0.6rem 1.25rem;
      border-radius: 999px;
      border: 1px solid var(--border-color);
    }

    .lightbox-title {
      font-size: 0.9rem;
      font-weight: 600;
    }

    .close-btn {
      position: absolute;
      top: -40px;
      right: 0;
      background: none;
      border: none;
      color: #fff;
      font-size: 1.5rem;
      cursor: pointer;
    }

    .nav-btn {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      background: rgba(30, 41, 59, 0.8);
      border: 1px solid var(--border-color);
      color: white;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 1.25rem;
      transition: background 0.2s;
    }

    .nav-btn:hover {
      background: var(--accent-cyan);
      color: #000;
    }

    .nav-prev { left: 1rem; }
    .nav-next { right: 1rem; }

    footer {
      background-color: var(--bg-surface);
      border-top: 1px solid var(--border-color);
      padding: 1.25rem;
      text-align: center;
      font-size: 0.8rem;
      color: var(--text-secondary);
      margin-top: auto;
    }

    @media (max-width: 640px) {
      .gallery-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 0.75rem;
      }
      .nav-btn { display: none; }
    }
  </style>
</head>
<body>

  <header>
    <div class="header-container">
      <div class="brand">
        <div class="brand-icon">⚡</div>
        <div class="brand-text">
          <h1>PhotoShare Hub</h1>
          <p>Local Wi-Fi Photo Explorer</p>
        </div>
      </div>
      <div class="header-actions">
        <button id="refreshBtn" class="btn btn-secondary" onclick="fetchPhotos()">
          🔄 Refresh
        </button>
        <a id="downloadZipBtn" href="/api/zip" class="btn btn-primary" download>
          📦 Download All (ZIP)
        </a>
      </div>
    </div>
  </header>

  <main class="main-container">
    <div class="controls-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input type="text" id="searchInput" placeholder="Search photos by filename..." oninput="onSearch(this.value)">
      </div>

      <div class="albums-nav" id="albumsNav">
        <div class="album-chip active" onclick="selectAlbum('All')">All Photos</div>
      </div>

      <div class="stats-info">
        <span class="status-dot"></span>
        <span id="photoCountText">Loading photos...</span>
      </div>
    </div>

    <div class="gallery-grid" id="galleryGrid">
      <div class="empty-state">
        <p>Loading photo gallery from phone...</p>
      </div>
    </div>
  </main>

  <div class="lightbox" id="lightbox" onclick="closeLightbox(event)">
    <button class="nav-btn nav-prev" onclick="prevPhoto(event)">❮</button>
    <div class="lightbox-content" onclick="event.stopPropagation()">
      <button class="close-btn" onclick="closeLightbox()">✕</button>
      <img id="lightboxImg" class="lightbox-img" src="" alt="Full Preview">
      <div class="lightbox-bar">
        <span id="lightboxTitle" class="lightbox-title">Photo</span>
        <a id="lightboxDownload" href="#" class="btn btn-primary" download style="padding: 0.4rem 0.8rem; font-size: 0.8rem;">
          ⬇ Download Original
        </a>
      </div>
    </div>
    <button class="nav-btn nav-next" onclick="nextPhoto(event)">❯</button>
  </div>

  <footer>
    <p>PhotoShare Hub &bull; Direct High-Performance Local Network Photo Streamer &bull; End-to-End Local Privacy</p>
  </footer>

  <script>
    let allPhotos = [];
    let filteredPhotos = [];
    let currentAlbum = 'All';
    let currentPhotoIndex = 0;

    async function fetchPhotos() {
      try {
        const res = await fetch('/api/photos');
        if (!res.ok) throw new Error('Failed to load photos');
        allPhotos = await res.json();
        renderAlbums();
        applyFilters();
      } catch (err) {
        console.error(err);
        document.getElementById('galleryGrid').innerHTML = '<div class="empty-state"><p>Failed to connect to phone photo server.</p></div>';
      }
    }

    function renderAlbums() {
      const albumsNav = document.getElementById('albumsNav');
      const albumCounts = {};
      allPhotos.forEach(p => {
        const a = p.album || 'Other';
        albumCounts[a] = (albumCounts[a] || 0) + 1;
      });

      const sortedAlbums = Object.keys(albumCounts).sort();
      let html = '<div class="album-chip ' + (currentAlbum === 'All' ? 'active' : '') + '" onclick="selectAlbum(\'All\')">All (' + allPhotos.length + ')</div>';

      sortedAlbums.forEach(a => {
        const isActive = currentAlbum === a ? 'active' : '';
        html += '<div class="album-chip ' + isActive + '" onclick="selectAlbum(\'' + a.replace(/'/g, "\\'") + '\')">' + a + ' (' + albumCounts[a] + ')</div>';
      });

      albumsNav.innerHTML = html;
    }

    function selectAlbum(album) {
      currentAlbum = album;
      renderAlbums();
      applyFilters();
      const zipBtn = document.getElementById('downloadZipBtn');
      if (album === 'All') {
        zipBtn.href = '/api/zip';
        zipBtn.innerText = '📦 Download All (ZIP)';
      } else {
        zipBtn.href = '/api/zip?album=' + encodeURIComponent(album);
        zipBtn.innerText = '📦 Download ' + album + ' (ZIP)';
      }
    }

    function onSearch(query) {
      applyFilters();
    }

    function applyFilters() {
      const query = (document.getElementById('searchInput').value || '').trim().toLowerCase();
      filteredPhotos = allPhotos.filter(p => {
        const matchAlbum = (currentAlbum === 'All') || (p.album === currentAlbum);
        const matchSearch = !query || p.name.toLowerCase().includes(query) || (p.date && p.date.toLowerCase().includes(query));
        return matchAlbum && matchSearch;
      });

      renderGallery();
    }

    function renderGallery() {
      const grid = document.getElementById('galleryGrid');
      const countText = document.getElementById('photoCountText');
      countText.innerText = filteredPhotos.length + ' of ' + allPhotos.length + ' photos';

      if (filteredPhotos.length === 0) {
        grid.innerHTML = '<div class="empty-state"><p>No photos found in this view.</p></div>';
        return;
      }

      let html = '';
      filteredPhotos.forEach((p, idx) => {
        html += '<div class="photo-card" onclick="openLightbox(' + idx + ')">';
        html += '  <div class="thumb-wrapper">';
        html += '    <img src="' + p.thumbUrl + '" loading="lazy" alt="' + p.name + '" onerror="this.src=\'' + p.url + '\'">';
        html += '    <span class="album-badge">' + (p.album || 'Gallery') + '</span>';
        html += '    <span class="size-badge">' + (p.sizeFormatted || '') + '</span>';
        html += '  </div>';
        html += '  <div class="card-info">';
        html += '    <div class="photo-title" title="' + p.name + '">' + p.name + '</div>';
        html += '    <div class="photo-meta">';
        html += '      <span>' + (p.date || '') + '</span>';
        html += '      <span>' + (p.resolution || '') + '</span>';
        html += '    </div>';
        html += '  </div>';
        html += '</div>';
      });

      grid.innerHTML = html;
    }

    function openLightbox(idx) {
      currentPhotoIndex = idx;
      const photo = filteredPhotos[idx];
      if (!photo) return;

      document.getElementById('lightboxImg').src = photo.url;
      document.getElementById('lightboxTitle').innerText = photo.name + ' (' + photo.sizeFormatted + ')';
      document.getElementById('lightboxDownload').href = photo.downloadUrl;
      document.getElementById('lightbox').classList.add('active');
    }

    function closeLightbox(e) {
      if (e && e.target && e.target.classList.contains('lightbox-content')) return;
      document.getElementById('lightbox').classList.remove('active');
    }

    function nextPhoto(e) {
      if (e) e.stopPropagation();
      if (filteredPhotos.length === 0) return;
      currentPhotoIndex = (currentPhotoIndex + 1) % filteredPhotos.length;
      openLightbox(currentPhotoIndex);
    }

    function prevPhoto(e) {
      if (e) e.stopPropagation();
      if (filteredPhotos.length === 0) return;
      currentPhotoIndex = (currentPhotoIndex - 1 + filteredPhotos.length) % filteredPhotos.length;
      openLightbox(currentPhotoIndex);
    }

    document.addEventListener('keydown', e => {
      if (!document.getElementById('lightbox').classList.contains('active')) return;
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowRight') nextPhoto();
      if (e.key === 'ArrowLeft') prevPhoto();
    });

    // Initial load
    fetchPhotos();
  </script>
</body>
</html>"""
    }
}
