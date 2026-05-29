package com.company.scraper.common.proxy;

import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.model.ProxyStatus;
import com.company.scraper.common.repository.ProxyEndpointRepository;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class ProxyPoolService {

    private final ProxyEndpointRepository repository;

    public ProxyPoolService(ProxyEndpointRepository repository) {
        this.repository = repository;
    }

    public ProxyEndpoint nextHealthy() {
        List<ProxyEndpoint> proxies = repository.findByStatus(ProxyStatus.HEALTHY);
        if (proxies.isEmpty()) {
            return null;
        }
        return proxies.get(ThreadLocalRandom.current().nextInt(proxies.size()));
    }
}
