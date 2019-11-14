package fr.leroymerlin.demo;

import fr.leroymerlin.demo.dto.FakeApplication;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.Closeable;
import java.io.IOException;

/**
 * This shows a very simplified method of registering an instance with the service discovery. Each individual
 * instance in your distributed set of applications would create an instance of something similar to ExampleServer,
 * start it when the application comes up and close it when the application shuts down.
 */
public class FakeApplicationInstance implements Closeable {

    private final ServiceDiscovery<FakeApplication> serviceDiscovery;
    private final ServiceInstance<FakeApplication> thisInstance;

    public FakeApplicationInstance(CuratorFramework client, String path, String serviceName, String url, int priority) throws Exception {

        // in a real application, you'd have a convention of some kind for the URI layout
        UriSpec uriSpec = new UriSpec(url);
        //int port = (int) (65535 * Math.random());
        int port = 80;

        thisInstance = ServiceInstance.<FakeApplication>builder()
                           .name(serviceName)
                           .payload(new FakeApplication(serviceName, url, priority))
                           .port(port) // in a real application, you'd use a common port
                           .uriSpec(uriSpec)
                           .build();

        // if you mark your payload class with @JsonRootName the provided JsonInstanceSerializer will work
        JsonInstanceSerializer<FakeApplication> serializer = new JsonInstanceSerializer<FakeApplication>(FakeApplication.class);

        serviceDiscovery = ServiceDiscoveryBuilder.builder(FakeApplication.class)
                                                  .client(client)
                                                  .basePath(path)
                                                  .serializer(serializer)
                                                  .thisInstance(thisInstance)
                                                  .build();
    }

    public ServiceInstance<FakeApplication> getThisInstance() {
        return thisInstance;
    }

    public void start() throws Exception {
        serviceDiscovery.start();
    }

    @Override
    public void close() throws IOException {
        CloseableUtils.closeQuietly(serviceDiscovery);
    }
}
