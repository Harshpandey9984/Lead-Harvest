- [ ] Confirm repo structure (already done: multi-module pom at root)
- [ ] Fix Docker build failure: root pom references modules that aren’t copied into Docker build context
- [ ] Update deploy/docker Dockerfiles to copy root multi-module project POM + all modules needed for Maven reactor
- [ ] Re-run `docker compose up -d --build`
- [ ] Verify containers start: `docker compose ps`
- [ ] Verify API reachable: `http://localhost:8080`
- [ ] Provide next commands to start scraping (API calls / testing)

