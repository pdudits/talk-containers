package fish.payara.demo.containers.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;

@Path("hello")
@RequestScoped
public class HelloWorldResource {

    @Inject
    @ConfigProperty(name = "hello.defaultName", defaultValue = "world")
    private String defaultName;

    @Inject
    @ConfigProperty(name = "hello.downstreamUri")
    Optional<URI> downstreamUri;

    @GET
    public Response hello(@QueryParam("name") String name, @HeaderParam("Upstream-ID") String upstreamId) {
        var response = new StringBuilder();
        response.append("Hello, ");
        if ((name == null) || name.trim().isEmpty()) {
            response.append(defaultName);
        } else {
            response.append(name);
        }
        response.append("!");
        downstreamUri.ifPresent(downstreamUri -> {
            if (upstreamId != null) {
                response.append("\nWill not make downstream call, this one already arrived from ")
                        .append(upstreamId);
                return;
            }
            var client = ClientBuilder.newClient().target(downstreamUri);
            var downstreamResponse = client.request(MediaType.TEXT_PLAIN)
                    .header("Upstream-ID", getHostname())
                    .get(String.class);
            response.append("\nDownstream says: ");
            response.append(downstreamResponse);
        });
        return Response
                .ok(response.toString())
                .build();
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

}