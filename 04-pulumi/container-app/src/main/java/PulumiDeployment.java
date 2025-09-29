import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.core.v1.NamespaceArgs;
import com.pulumi.kubernetes.meta.v1.inputs.ObjectMetaArgs;
import com.pulumi.resources.CustomResourceOptions;
import containers.deployment.ContainerApp;


public class PulumiDeployment {
    public static void main(String[] args) {
        Pulumi.run(PulumiDeployment::run);
    }

    static void run(Context ctx) {
        // Pulumi collects state by invoking constructors of resource objects
        var config = ctx.config();
        var namespaceName = config.get("namespace").orElse("demonstration-pulumi");
        var ns = new Namespace(namespaceName, NamespaceArgs.builder()
                .metadata(ObjectMetaArgs.builder()
                        // provide explicit name, otherwise an ID is suffixed to the name
                        .name(namespaceName)
                        .build())
                .build());
        var hostname = config.get("hostName").orElse("containers-pulumi.payara.app");
        var instance1 = new ContainerApp("first-instance", ns,
                hostname,
                "/");
        var instance2 = new ContainerApp("second-instance", ns, hostname,
                "/second", instance1.internalEndpoint());

        ctx.export("firstEndpoint", instance1.endpoint());
        ctx.export("secondEndpoint", instance2.endpoint());
    }
}