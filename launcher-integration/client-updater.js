'use strict';
/**
 * Atenas Client - actualizador automatico para el launcher (Node/Electron, sin dependencias).
 * Descarga el jar de la ultima Release de GitHub a la carpeta mods de la instancia
 * y borra versiones anteriores. Si no hay internet y ya hay un jar, deja el que esta.
 *
 * Uso en el launcher (antes de lanzar el juego):
 *   const { ensureClient } = require('./client-updater');
 *   const r = await ensureClient({
 *     repo: 'TU-USUARIO/AtenasClient',
 *     modsDir: path.join(gameDir, 'mods'),
 *     onProgress: (done, total) => { // actualiza tu barra }
 *   });
 */
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

function get(url, headers, redirects) {
  if (redirects === undefined) redirects = 5;
  return new Promise(function (resolve, reject) {
    const lib = url.indexOf('https:') === 0 ? https : http;
    const req = lib.get(url, { headers: Object.assign({ 'User-Agent': 'AtenasLauncher' }, headers || {}) }, function (res) {
      if ([301, 302, 303, 307, 308].indexOf(res.statusCode) >= 0 && res.headers.location && redirects > 0) {
        res.resume();
        return resolve(get(new URL(res.headers.location, url).toString(), headers, redirects - 1));
      }
      resolve(res);
    });
    req.setTimeout(15000, function () { req.destroy(new Error('timeout')); });
    req.on('error', reject);
  });
}

async function getJson(url) {
  const res = await get(url, { Accept: 'application/vnd.github+json' });
  if (res.statusCode !== 200) {
    res.resume();
    throw new Error('HTTP ' + res.statusCode + ' al consultar ' + url);
  }
  const chunks = [];
  for await (const c of res) chunks.push(c);
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

async function download(url, dest, expectedSize, onProgress) {
  const res = await get(url);
  if (res.statusCode !== 200) {
    res.resume();
    throw new Error('HTTP ' + res.statusCode + ' al descargar el cliente');
  }
  const total = expectedSize || parseInt(res.headers['content-length'] || '0', 10);
  let done = 0;
  await new Promise(function (resolve, reject) {
    const out = fs.createWriteStream(dest);
    res.on('data', function (c) { done += c.length; if (onProgress) onProgress(done, total); });
    res.on('error', reject);
    out.on('error', reject);
    out.on('finish', resolve);
    res.pipe(out);
  });
  if (expectedSize && fs.statSync(dest).size !== expectedSize) {
    throw new Error('Descarga incompleta (tamano distinto al esperado)');
  }
}

async function ensureClient(opts) {
  const repo = opts.repo;
  const modsDir = opts.modsDir;
  const apiBase = opts.apiBase || 'https://api.github.com';
  fs.mkdirSync(modsDir, { recursive: true });

  const metaPath = path.join(modsDir, '.atenas-client.json');
  let meta = {};
  try { meta = JSON.parse(fs.readFileSync(metaPath, 'utf8')); } catch (e) { /* primera vez */ }
  const haveJar = !!(meta.file && fs.existsSync(path.join(modsDir, meta.file)));

  let release;
  try {
    release = await getJson(apiBase + '/repos/' + repo + '/releases/latest');
  } catch (e) {
    if (haveJar) return { updated: false, offline: true, tag: meta.tag, file: meta.file };
    throw e;
  }

  const asset = (release.assets || []).filter(function (a) { return /^AtenasClient-[\d.]+\.jar$/.test(a.name); })[0];
  if (!asset) {
    if (haveJar) return { updated: false, tag: meta.tag, file: meta.file };
    throw new Error('La release ' + release.tag_name + ' no incluye AtenasClient-X.Y.Z.jar');
  }
  if (haveJar && meta.tag === release.tag_name && meta.file === asset.name) {
    return { updated: false, tag: meta.tag, file: meta.file };
  }

  const tmp = path.join(modsDir, asset.name + '.part');
  try {
    await download(asset.browser_download_url, tmp, asset.size, opts.onProgress);
  } catch (e) {
    try { fs.unlinkSync(tmp); } catch (x) { /* nada */ }
    if (haveJar) return { updated: false, error: String(e.message), tag: meta.tag, file: meta.file };
    throw e;
  }

  fs.readdirSync(modsDir).forEach(function (f) {
    if (/^AtenasClient-.*\.jar$/.test(f)) {
      try { fs.unlinkSync(path.join(modsDir, f)); } catch (e) { /* en uso */ }
    }
  });
  fs.renameSync(tmp, path.join(modsDir, asset.name));
  fs.writeFileSync(metaPath, JSON.stringify({ tag: release.tag_name, file: asset.name }));
  return { updated: true, tag: release.tag_name, file: asset.name };
}

module.exports = { ensureClient };
