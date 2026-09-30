/* Service worker de CitrusCode: permite instalar la app y abrirla sin conexión.
   La página siempre se pide primero a la red (así cada actualización se ve al
   instante) y solo si no hay conexión se usa la copia guardada. Firebase y las
   librerías externas no pasan por acá. */
const CACHE = 'citruscode-v1';
const BASE = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './apple-touch-icon.png'];

self.addEventListener('install', function(e){
  e.waitUntil(caches.open(CACHE).then(function(c){ return c.addAll(BASE); }).then(function(){ return self.skipWaiting(); }));
});

self.addEventListener('activate', function(e){
  e.waitUntil(caches.keys().then(function(claves){
    return Promise.all(claves.filter(function(k){ return k !== CACHE; }).map(function(k){ return caches.delete(k); }));
  }).then(function(){ return self.clients.claim(); }));
});

self.addEventListener('fetch', function(e){
  const req = e.request;
  if(req.method !== 'GET' || new URL(req.url).origin !== self.location.origin) return;
  if(req.mode === 'navigate'){
    e.respondWith(fetch(req).then(function(res){
      const copia = res.clone();
      caches.open(CACHE).then(function(c){ c.put('./index.html', copia); });
      return res;
    }).catch(function(){ return caches.match('./index.html'); }));
    return;
  }
  e.respondWith(caches.match(req).then(function(guardado){
    const red = fetch(req).then(function(res){
      if(res.ok){ const copia = res.clone(); caches.open(CACHE).then(function(c){ c.put(req, copia); }); }
      return res;
    }).catch(function(){ return guardado; });
    return guardado || red;
  }));
});
