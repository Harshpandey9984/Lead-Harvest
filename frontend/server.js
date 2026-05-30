const express = require('express');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Routes
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// API proxy for development
app.all('/api/*', (req, res) => {
  // Forward to backend API
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: req.path,
    method: req.method,
    headers: req.headers
  };

  const http = require('http');
  const backendReq = http.request(options, (backendRes) => {
    let data = '';
    backendRes.on('data', (chunk) => {
      data += chunk;
    });
    backendRes.on('end', () => {
      res.status(backendRes.statusCode).send(data);
    });
  });

  backendReq.on('error', (error) => {
    console.error('Backend request error:', error);
    res.status(503).json({ error: 'Backend service unavailable' });
  });

  if (req.body) {
    backendReq.write(JSON.stringify(req.body));
  }
  backendReq.end();
});

app.listen(PORT, () => {
  console.log(`Frontend server running at http://localhost:${PORT}`);
  console.log(`Backend API: http://localhost:8080`);
});
