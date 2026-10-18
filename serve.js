// Tiny static server so the simulator runs live in the Claude app's browser pane: node serve.js [port]
const http = require('http'), fs = require('fs'), path = require('path');
const root = path.join(__dirname, 'house-monitor'), port = +process.argv[2] || 8450;
const types = {'.html': 'text/html; charset=utf-8', '.txt': 'text/plain; charset=utf-8'};
http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (p === '/') p = '/House-Monitor.html';
  const f = path.join(root, p);
  if (!f.startsWith(root)) { res.writeHead(403); return res.end(); }
  fs.readFile(f, (err, data) => {
    if (err) { res.writeHead(404); return res.end('not found'); }
    res.writeHead(200, {'Content-Type': types[path.extname(f)] || 'application/octet-stream', 'Cache-Control': 'no-store'});
    res.end(data);
  });
}).listen(port, '127.0.0.1', () => console.log('House Monitor on http://localhost:' + port));
