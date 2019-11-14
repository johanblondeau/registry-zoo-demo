package fr.leroymerlin.demo.registry;

import com.google.common.collect.Maps;
import fr.leroymerlin.demo.dto.FakeApplication;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RegistryService {

    @Value("${registry.path}")
    private String path;

    @Value("${registry.connect-string}")
    private String connectString;

    /**
     * The zooKeeper client.
     */
    private CuratorFramework client = null;

    /**
     * Service Discovery.
     */
    private ServiceDiscovery<FakeApplication> serviceDiscovery = null;

    /**
     * List of registered applications.
     */
    private Map<String, List<FakeApplication>> registeredApplications = Maps.newHashMap();

    /**
     * Create zookeeper connection with curator framework.
     */
    private CuratorFramework newClient() {
        int sleepMsBetweenRetries = 100;
        int maxRetries = 3;
        RetryPolicy retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);
        return CuratorFrameworkFactory.newClient(connectString, retryPolicy);
    }

    @PostConstruct
    public void startServiceDiscovery() {
        try {

            // First, create session
            client = newClient();
            client.start();

            // Create and start service discovery based on custom model
            JsonInstanceSerializer<FakeApplication> serializer = new JsonInstanceSerializer<FakeApplication>(FakeApplication.class);
            serviceDiscovery = ServiceDiscoveryBuilder.builder(FakeApplication.class)
                                                      .client(client)
                                                      .basePath(path)
                                                      .serializer(serializer)
                                                      .build();
            serviceDiscovery.start();

            // Listener for all zookeeper events
            watchRegistryEvents();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void closeRegistry() {
        CloseableUtils.closeQuietly(serviceDiscovery);
        CloseableUtils.closeQuietly(client);
    }

    /**
     * Return the list of registered applications.
     */
    public void listRegisteredApplications() {
        try {
            Collection<String> serviceNames = serviceDiscovery.queryForNames();
            log.info(serviceNames.size() + " type(s)");
            if (!CollectionUtils.isEmpty(serviceNames)) {
                registeredApplications = Maps.newHashMap();
                for (String serviceName : serviceNames) {
                    List<FakeApplication> apps = new ArrayList<>();
                    Collection<ServiceInstance<FakeApplication>> instances = serviceDiscovery.queryForInstances(serviceName);
                    for (ServiceInstance<FakeApplication> instance : instances) {
                        apps.add(instance.getPayload());
                    }
                    registeredApplications.put(serviceName, apps);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void watchRegistryEvents() throws Exception {

        TreeCache cache = TreeCache.newBuilder(client, path)
                                   .setCacheData(false)
                                   .build();
        cache.getListenable()
             .addListener((c, event) -> {
                 if (event.getData() != null) {
                     log.info("type=" + event.getType() + " path=" + event.getData()
                                                                          .getPath());
                 } else {
                     log.info("type=" + event.getType());
                 }
                 // refresh list
                 listRegisteredApplications();
             });
        cache.start();

    }

    public Map<String, List<FakeApplication>> getRegisteredApplications() {
        return registeredApplications;
    }

    public void setRegisteredApplications(Map<String, List<FakeApplication>> registeredApplications) {
        this.registeredApplications = registeredApplications;
    }

}
