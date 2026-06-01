# Vercel deployment (full stack)

This repo contains a **frontend** (`frontend/`) and **backend** Java services (`scraper-api`, `scraper-worker`, `scraper-scheduler`) that require **Kafka + Redis + Postgres**.

Vercel cannot natively run the Spring Boot + Kafka/Redis/Postgres stack.

## Recommended deployment model
1. **Deploy `frontend/` to Vercel**
2. **Deploy backend using Docker** to a platform that supports Docker containers + services (e.g. Fly.io, Render, Railway, ECS, Kubernetes)
3. Set frontend to call the backend base URL.

## Backend base URL wiring
- In this repo, the frontend is configured to use `BACKEND_ORIGIN = 'http://localhost:3100'` inside `frontend/public/index.html`.
- After deploying backend, update that value to your backend URL.

## Docker support already present
- Dockerfiles exist in `deploy/docker/`.
- `docker-compose.yml` defines the full stack (postgres/redis/zookeeper/kafka + scraper-api/worker/scheduler).

## Next steps to create Vercel config
- Add a Vercel configuration to build/serve `frontend/public/index.html` (static) and expose any needed API routes if you want to keep `frontend/server.js`.
- Create a runtime config (environment variable) for the backend origin.

