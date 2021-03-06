package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;

public class HealthServiceContext implements ServiceContext<HealthCheckService> {

    private static final Logger LOG = LoggerFactory.getLogger(HealthServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/services/HealthCheckService";
    private final ServiceRegistry registry;
    private final HealthCheckService service;

    @Autowired
    public HealthServiceContext(ServiceRegistry registry,HealthCheckService service){

        this.registry = registry;
        this.service = service;
    }

    private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public HealthCheckService getService() {
        return service;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOG.debug("Initializing context for Health Check Service");
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOG.debug("Destroying context for Health Check Service");
    }
}
