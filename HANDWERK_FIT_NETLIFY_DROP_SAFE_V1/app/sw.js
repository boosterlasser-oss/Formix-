const CACHE = 'hwfit-pwa-v1-netlify-new';
const ASSETS=["./", "./index.html", "./assets/styles.css", "./assets/app.js", "./exercise.html", "./manifest.webmanifest", "./icon.png", "./icon-192.png", "./icon-256.png", "./animations/anim_map.json"];
self.addEventListener("install", e=>{
  e.waitUntil((async()=>{const c=await caches.open(CACHE); await c.addAll(ASSETS); self.skipWaiting();})());
});
self.addEventListener("activate", e=>{
  e.waitUntil((async()=>{const keys=await caches.keys(); await Promise.all(keys.map(k=>k!==CACHE?caches.delete(k):Promise.resolve())); self.clients.claim();})());
});
self.addEventListener("fetch", e=>{
  e.respondWith((async()=>{const hit=await caches.match(e.request); if(hit) return hit; try{return await fetch(e.request);}catch(err){return caches.match("./index.html");}})());
});
