package containers.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.resources.ResourceArgs;

public class ContainerAppArgs extends ResourceArgs {
    private final Output<String> namespace;
    @Import(required = true)
    private Output<String> hostName;

    @Import
    private Output<String> contextRoot;

    @Import
    private Output<String> downstream;

    public ContainerAppArgs(Output<String> namespace, String hostName, String contextRoot, Output<String> downstream) {
        this.namespace = namespace;
        this.hostName = Output.of(hostName);
        this.contextRoot = Output.of(contextRoot);
        this.downstream = downstream;
    }

    public Output<String> downstream() {
        return downstream;
    }

    public Output<String> namespace() {
        return namespace;
    }

    public Output<String> contextRoot() {
        return contextRoot;
    }

    public Output<String> hostName() {
        return hostName;
    }
}
