/* Service worker de CitrusCode: permite instalar la app y usarla sin conexión.
   La página siempre se pide primero a la red (así cada actualización se ve al
   instante) y solo si no hay conexión se usa la copia guardada. Las librerías
   externas (Firebase, PDF, mapas, QR, fuentes) se guardan la primera vez para
   que el sistema abra igual en la finca sin señal. Los datos no pasan por acá:
   de eso se encarga la caché de Firestore. */
const CACHE = 'citruscode-v2';
const BASE = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './apple-touch-icon.png'];
const EXTERNAS = [
  'https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js',
  'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth-compat.js',
  'https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore-compat.js',
  'https://www.gstatic.com/firebasejs/10.14.1/firebase-storage-compat.js',
  'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js',
  'https://cdnjs.cloudflare.com/ajax/libs/qrcode-generator/1.4.4/qrcode.min.js',
  'https://cdnjs.cloudflare.com/ajax/libs/jsQR/1.4.0/jsQR.min.js'
];
// Solo estos orígenes se guardan; Firestore, Auth y Storage van siempre a la red.
const CDN = ['https://www.gstatic.com/firebasejs/', 'https://cdnjs.cloudflare.com/', 'https://cdn.jsdelivr.net/npm/', 'https://fonts.googleapis.com/', 'https://fonts.gstatic.com/'];

self.addEventListener('install', function(e){
  e.waitUntil(caches.open(CACHE).then(function(c){
    // Las externas se piden de a una: si alguna falla, no frena la instalación.
    return c.addAll(BASE).then(function(){
      return Promise.all(EXTERNAS.map(function(url){
        return fetch(url, { mode: 'no-cors' }).then(function(res){ return c.put(url, res); }).catch(function(){});
      }));
    });
  }).then(function(){ return self.skipWaiting(); }));
});

self.addEventListener('activate', function(e){
  e.waitUntil(caches.keys().then(function(claves){
    return Promise.all(claves.filter(function(k){ return k !== CACHE; }).map(function(k){ return caches.delete(k); }));
  }).then(function(){ return self.clients.claim(); }));
});

self.addEventListener('fetch', function(e){
  const req = e.request;
  if(req.method !== 'GET') return;
  const propia = new URL(req.url).origin === self.location.origin;
  const externa = CDN.some(function(p){ return req.url.indexOf(p) === 0; });
  if(!propia && !externa) return;
  if(propia && req.mode === 'navigate'){
    e.respondWith(fetch(req).then(function(res){
      const copia = res.clone();
      caches.open(CACHE).then(function(c){ c.put('./index.html', copia); });
      return res;
    }).catch(function(){ return caches.match('./index.html'); }));
    return;
  }
  e.respondWith(caches.match(req, { ignoreVary: true }).then(function(guardado){
    const red = fetch(req).then(function(res){
      if(res.ok || res.type === 'opaque'){ const copia = res.clone(); caches.open(CACHE).then(function(c){ c.put(req, copia); }); }
      return res;
    }).catch(function(){ return guardado; });
    return guardado || red;
  }));
});
