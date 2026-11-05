/* Patient Monitor offline worker: the whole monitor is one page, so once saved it runs with no internet.
   Online the page is fetched fresh (and re-saved) so updates arrive; offline or on a stalled connection the saved copy is used. */
const CACHE='patientmonitor-2026-11-05.3';
self.addEventListener('install',e=>{ e.waitUntil(caches.open(CACHE).then(c=>c.addAll(['./','manifest.webmanifest','icon-192.png','icon-512.png'])).then(()=>self.skipWaiting())) });
self.addEventListener('activate',e=>{ e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE&&k.startsWith('patientmonitor-')).map(k=>caches.delete(k)))).then(()=>self.clients.claim())) });
self.addEventListener('fetch',e=>{ const r=e.request; if(r.method!=='GET'||new URL(r.url).origin!==location.origin) return;
  if(r.mode==='navigate'){ const net=fetch(r,{cache:'no-store'}).then(res=>{ if(res.ok){ const c=res.clone(); caches.open(CACHE).then(ca=>ca.put('./',c)) } return res });
    const slow=new Promise(ok=>setTimeout(ok,4000)).then(()=>caches.match('./'));
    e.respondWith(Promise.race([net.catch(()=>caches.match('./')),slow.then(hit=>hit||net)])); return }
  e.respondWith(caches.match(r).then(hit=>hit||fetch(r))) });
