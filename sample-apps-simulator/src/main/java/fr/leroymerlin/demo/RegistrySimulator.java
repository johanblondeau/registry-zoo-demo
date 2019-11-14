package fr.leroymerlin.demo;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.leroymerlin.demo.dto.FakeApplication;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RegistrySimulator {

    private static final String PATH = "/kobi";

    public static void main(String[] args) throws Exception {

        // This method is scaffolding to get the example up and running
        CuratorFramework client = null;
        ServiceDiscovery<FakeApplication> serviceDiscovery = null;
        Map<String, ServiceProvider<FakeApplication>> providers = Maps.newHashMap();
        try {
            client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", new ExponentialBackoffRetry(1000, 3));
            client.start();

            JsonInstanceSerializer<FakeApplication> serializer = new JsonInstanceSerializer<FakeApplication>(FakeApplication.class);
            serviceDiscovery = ServiceDiscoveryBuilder.builder(FakeApplication.class)
                                                      .client(client)
                                                      .basePath(PATH)
                                                      .serializer(serializer)
                                                      .build();
            serviceDiscovery.start();

            processCommands(serviceDiscovery, providers, client);

        } finally {
            for (ServiceProvider<FakeApplication> cache : providers.values()) {
                CloseableUtils.closeQuietly(cache);
            }

            CloseableUtils.closeQuietly(serviceDiscovery);
            CloseableUtils.closeQuietly(client);
        }
    }

    private static void processCommands(ServiceDiscovery<FakeApplication> serviceDiscovery, Map<String, ServiceProvider<FakeApplication>> providers,
                                        CuratorFramework client) throws Exception {

        // More scaffolding that does a simple command line processor
        printHelp();

        List<FakeApplicationInstance> applications = Lists.newArrayList();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            boolean done = false;
            while (!done) {
                System.out.print("> ");

                String line = in.readLine();
                if (line == null) {
                    break;
                }

                String command = line.trim();
                String[] parts = command.split("\\s");
                if (parts.length == 0) {
                    continue;
                }
                String operation = parts[0];
                String args[] = Arrays.copyOfRange(parts, 1, parts.length);

                if (operation.equalsIgnoreCase("help") || operation.equalsIgnoreCase("?")) {
                    printHelp();
                } else if (operation.equalsIgnoreCase("q") || operation.equalsIgnoreCase("quit")) {
                    done = true;
                } else if (operation.equals("add")) {
                    addInstance(args, client, command, applications);
                } else if (operation.equals("delete")) {
                    deleteInstance(args, command, applications);
                } else if (operation.equals("list")) {
                    listInstances(serviceDiscovery);
                }
            }
        } finally {
            for (FakeApplicationInstance application : applications) {
                CloseableUtils.closeQuietly(application);
            }
        }
    }

    private static void listInstances(ServiceDiscovery<FakeApplication> serviceDiscovery) throws Exception {
        // This shows how to query all the instances in service discovery

        try {
            Collection<String> serviceNames = serviceDiscovery.queryForNames();
            System.out.println(serviceNames.size() + " type(s)");
            for (String serviceName : serviceNames) {
                Collection<ServiceInstance<FakeApplication>> instances = serviceDiscovery.queryForInstances(serviceName);
                System.out.println(serviceName);
                for (ServiceInstance<FakeApplication> instance : instances) {
                    outputInstance(instance);
                }
            }
        } finally {
            CloseableUtils.closeQuietly(serviceDiscovery);
        }
    }

    private static void outputInstance(ServiceInstance<FakeApplication> instance) {
        System.out.println("\t" + instance.getPayload()
                                          .getName() + "(" + instance.getUriSpec().build() +  ") : " + instance.getPayload().getUrl() + " [" + instance.getPayload().getPriority() + "]");
    }

    private static void deleteInstance(String[] args, String command, List<FakeApplicationInstance> applications) {
        // simulate a random instance going down
        // in a real application, this would occur due to normal operation, a crash, maintenance, etc.

        if (args.length != 1) {
            System.err.println("syntax error (expected delete <name>): " + command);
            return;
        }

        final String serviceName = args[0];
        FakeApplicationInstance application = Iterables.find
                                             (
                                                 applications,
                                                 new Predicate<FakeApplicationInstance>() {
                                                     @Override
                                                     public boolean apply(FakeApplicationInstance application) {
                                                         return application.getThisInstance()
                                                                      .getName()
                                                                      .endsWith(serviceName);
                                                     }
                                                 },
                                                 null
                                             );
        if (application == null) {
            System.err.println("No instances found named: " + serviceName);
            return;
        }

        applications.remove(application);
        CloseableUtils.closeQuietly(application);
        System.out.println("Removed a random instance of: " + serviceName);
    }

    private static void addInstance(String[] args, CuratorFramework client, String command, List<FakeApplicationInstance> applications) throws Exception {
        // simulate a new instance coming up
        // in a real application, this would be a separate process

        if (args.length < 2) {
            System.err.println("syntax error (expected add <name> <url> <priority>): " + command);
            return;
        }

        String serviceName = args[0];
        String url =  args[1];
        int priority = Integer.parseInt(args[2]);

        FakeApplicationInstance application = new FakeApplicationInstance(client, PATH, serviceName, url, priority);
        applications.add(application);
        application.start();

        System.out.println(serviceName + " added");
    }

    private static void printHelp() {
        System.out.println("An example of using the ServiceDiscovery APIs. This example is driven by entering commands at the prompt:\n");
        System.out.println("add <name> <url> <priority>: Adds a mock application with the given name, url and priority");
        System.out.println("delete <name>: Deletes one of the mock application with the given name");
        System.out.println("list: Lists all the currently registered services");
        System.out.println("quit: Quit the example");
        System.out.println();
    }
}
