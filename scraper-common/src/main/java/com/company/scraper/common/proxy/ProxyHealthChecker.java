package com.company.scraper.common.proxy;

import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.model.ProxyStatus;
import com.company.scraper.common.repository.ProxyEndpointRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ProxyHealthChecker {

    private final ProxyEndpointRepository repository;

    public ProxyHealthChecker(ProxyEndpointRepository repository) {
        this.repository = repository;
    }

    public void markSuccess(ProxyEndpoint proxy) {
        update(proxy, true);
    }

    public void markFailure(ProxyEndpoint proxy) {
        update(proxy, false);
    }

    private void update(ProxyEndpoint proxy, boolean success) {
        if (proxy == null) {
            return;
        }
        double rate = proxy.getSuccessRate() == null ? 1.0 : proxy.getSuccessRate();
        rate = success ? Math.min(1.0, rate + 0.05) : Math.max(0.0, rate - 0.1);
        proxy.setSuccessRate(rate);
        proxy.setStatus(rate < 0.2 ? ProxyStatus.BANNED : ProxyStatus.HEALTHY);
        proxy.setLastCheckedAt(Instant.now());
        repository.save(proxy);
    }
}
