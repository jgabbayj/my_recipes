import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    {
      name: 'fetch-url-proxy',
      configureServer(server) {
        server.middlewares.use(async (req, res, next) => {
          if (req.url.startsWith('/api/fetch-url')) {
            const urlParam = new URL(req.url, 'http://localhost').searchParams.get('url');
            if (!urlParam) {
              res.statusCode = 400;
              res.setHeader('Content-Type', 'application/json');
              res.end(JSON.stringify({ error: 'URL is required' }));
              return;
            }
            try {
              // Add simple user-agent to prevent websites from blocking the request
              const fetchResponse = await fetch(urlParam, {
                headers: {
                  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
                }
              });
              
              if (!fetchResponse.ok) {
                throw new Error(`Failed to fetch page: status ${fetchResponse.status}`);
              }
              
              const html = await fetchResponse.text();
              
              // Clean up HTML content to keep it small and light for Gemini
              const cleanText = html
                .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // remove scripts
                .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')   // remove styles
                .replace(/<header\b[^<]*(?:(?!<\/header>)<[^<]*)*<\/header>/gi, '') // remove headers
                .replace(/<footer\b[^<]*(?:(?!<\/footer>)<[^<]*)*<\/footer>/gi, '') // remove footers
                .replace(/<nav\b[^<]*(?:(?!<\/nav>)<[^<]*)*<\/nav>/gi, '')       // remove navs
                .replace(/<[^>]+>/g, ' ')                                           // strip HTML tags
                .replace(/\s+/g, ' ')                                               // collapse whitespace
                .trim();
                
              res.setHeader('Content-Type', 'application/json');
              res.end(JSON.stringify({ text: cleanText.slice(0, 50000) })); // limit output
            } catch (err) {
              res.statusCode = 500;
              res.setHeader('Content-Type', 'application/json');
              res.end(JSON.stringify({ error: err.message }));
            }
            return;
          }
          next();
        });
      }
    }
  ],
})

