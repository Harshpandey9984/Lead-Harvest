package com.company.scraper.common.proxy;

import com.company.scraper.common.model.ProxyEndpoint;
import org.springframework.stereotype.Service;

@Service
public class ProxyRotationService {

    private final ProxyPoolService poolService;

    public ProxyRotationService(ProxyPoolService poolService) {
        this.poolService = poolService;
    }

    public ProxyEndpoint assign() {
        return poolService.nextHealthy();
    }
}
