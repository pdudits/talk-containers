package containers.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.kubernetes.apps.v1.Deployment;
import com.pulumi.kubernetes.apps.v1.DeploymentArgs;
import com.pulumi.kubernetes.apps.v1.inputs.DeploymentSpecArgs;
import com.pulumi.kubernetes.core.v1.ConfigMap;
import com.pulumi.kubernetes.core.v1.ConfigMapArgs;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.core.v1.Service;
import com.pulumi.kubernetes.core.v1.ServiceArgs;
import com.pulumi.kubernetes.core.v1.inputs.ConfigMapVolumeSourceArgs;
import com.pulumi.kubernetes.core.v1.inputs.ContainerArgs;
import com.pulumi.kubernetes.core.v1.inputs.ContainerPortArgs;
import com.pulumi.kubernetes.core.v1.inputs.HTTPGetActionArgs;
import com.pulumi.kubernetes.core.v1.inputs.KeyToPathArgs;
import com.pulumi.kubernetes.core.v1.inputs.PodSpecArgs;
import com.pulumi.kubernetes.core.v1.inputs.PodTemplateSpecArgs;
import com.pulumi.kubernetes.core.v1.inputs.ProbeArgs;
import com.pulumi.kubernetes.core.v1.inputs.ResourceRequirementsArgs;
import com.pulumi.kubernetes.core.v1.inputs.SecretVolumeSourceArgs;
import com.pulumi.kubernetes.core.v1.inputs.ServicePortArgs;
import com.pulumi.kubernetes.core.v1.inputs.ServiceSpecArgs;
import com.pulumi.kubernetes.core.v1.inputs.VolumeArgs;
import com.pulumi.kubernetes.core.v1.inputs.VolumeMountArgs;
import com.pulumi.kubernetes.meta.v1.inputs.LabelSelectorArgs;
import com.pulumi.kubernetes.meta.v1.inputs.ObjectMetaArgs;
import com.pulumi.kubernetes.meta.v1.outputs.ObjectMeta;
import com.pulumi.kubernetes.networking.v1.Ingress;
import com.pulumi.kubernetes.networking.v1.IngressArgs;
import com.pulumi.kubernetes.networking.v1.inputs.HTTPIngressPathArgs;
import com.pulumi.kubernetes.networking.v1.inputs.HTTPIngressRuleValueArgs;
import com.pulumi.kubernetes.networking.v1.inputs.IngressBackendArgs;
import com.pulumi.kubernetes.networking.v1.inputs.IngressRuleArgs;
import com.pulumi.kubernetes.networking.v1.inputs.IngressServiceBackendArgs;
import com.pulumi.kubernetes.networking.v1.inputs.IngressSpecArgs;
import com.pulumi.kubernetes.networking.v1.inputs.ServiceBackendPortArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

public class ContainerApp extends ComponentResource {
    @Export
    final Output<String> endpoint;
    @Export
    private final Output<String> internalEndpoint;

    public Output<String> endpoint() {
        return endpoint;
    }

    public Output<String> internalEndpoint() {
        return internalEndpoint;
    }

    public ContainerApp(String name, ContainerAppArgs args, ComponentResourceOptions options) {
        super("talk:containers:ContainerApp", name, args, options);

        var imYourFather = CustomResourceOptions.builder().parent(this).build();

        var objectMetaDefault = ObjectMetaArgs.builder().namespace(args.namespace())
                .labels(Map.of("app", name))
                .build();

        var configMap = new ConfigMap(name+"-configmap", ConfigMapArgs.builder()
                .metadata(objectMetaDefault)
                .data(generateConfig(name,args).applyValue(s -> Map.of("application.properties", s)))
                .build(),
                imYourFather);

        var deployment = new Deployment(name, DeploymentArgs.builder()
                .metadata(objectMetaDefault)
                .spec(DeploymentSpecArgs.builder()
                        .replicas(1)
                        .selector(LabelSelectorArgs.builder().matchLabels(Map.of("app", name)).build())
                        .template(PodTemplateSpecArgs.builder()
                                .metadata(objectMetaDefault)
                                .spec(PodSpecArgs.builder()
                                        .volumes(VolumeArgs.builder()
                                                .name("config")
                                                .configMap(ConfigMapVolumeSourceArgs.builder()
                                                        .name(nameOf(configMap.metadata()))
                                                        .items(KeyToPathArgs.builder()
                                                                .path("application.properties")
                                                                .key("application.properties")
                                                                .build())
                                                        .build())
                                                .build())
                                        .containers(ContainerArgs.builder()
                                                .name("container-app")
                                                .image("pdudits/container-talk-demo:latest")
                                                .args(Output.<String>listBuilder()
                                                        .add("--deploymentDir", "/opt/payara/deployments", "--contextRoot")
                                                        .add(args.contextRoot())
                                                        .add( "--systemProperties", "/config/application.properties")
                                                        .build())
                                                .ports(ContainerPortArgs.builder()
                                                        .name("http")
                                                        .containerPort(8080)
                                                        .protocol("TCP")
                                                        .build())
                                                .volumeMounts(VolumeMountArgs.builder()
                                                        .name("config")
                                                        .mountPath("/config")
                                                        .build())
                                                .resources(ResourceRequirementsArgs.builder()
                                                        .requests(Map.of("cpu","250m", "memory", "256Mi"))
                                                        .build())
                                                .livenessProbe(ProbeArgs.builder()
                                                        .httpGet(HTTPGetActionArgs.builder()
                                                                .path("/health/live")
                                                                .port("http")
                                                                .build())
                                                        .build())
                                                .readinessProbe(ProbeArgs.builder()
                                                        .httpGet(HTTPGetActionArgs.builder()
                                                                .path("/health/ready")
                                                                .port("http")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build(), imYourFather);

        var service = new Service(name, ServiceArgs.builder()
                .metadata(ObjectMetaArgs.builder(objectMetaDefault).name(name).build())
                .spec(ServiceSpecArgs.builder()
                        .type("ClusterIP")
                        .selector(Map.of("app", name))
                        .ports(ServicePortArgs.builder()
                                .name("http")
                                .port(80)
                                .targetPort("http")
                                .build())
                        .build()).build()
                , imYourFather);

        var ingress = new Ingress(name, IngressArgs.builder()
                .metadata(objectMetaDefault)
                .spec(IngressSpecArgs.builder()
                        .ingressClassName("traefik-cloud-head")
                        .rules(IngressRuleArgs.builder()
                                .host(args.hostName())
                                .http(HTTPIngressRuleValueArgs.builder()
                                        .paths(HTTPIngressPathArgs.builder()
                                                .path(args.contextRoot())
                                                .pathType("Prefix")
                                                .backend(IngressBackendArgs.builder()
                                                        .service(IngressServiceBackendArgs.builder()
                                                                .name(nameOf(service.metadata()))
                                                                .port(ServiceBackendPortArgs.builder()
                                                                        .name("http")
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build(), imYourFather);
        this.endpoint = Output.format("https://%s%s/hello", args.hostName(), args.contextRoot());
        this.internalEndpoint = Output.format("http://%s%s/hello", nameOf(service.metadata()), args.contextRoot());
    }

    private Output<String> nameOf(Output<ObjectMeta> meta) {
        return meta.applyValue(m -> m.name().orElseThrow());
    }

    private Output<String> generateConfig(String name, ContainerAppArgs args) {
        var props = new Properties();
        props.put("hello.defaultName", name);
        if (args.downstream() != null) {
            // One needs to bow to the Output monad (it's potentially asynchronous)
            return args.downstream().applyValue(u -> {
                props.put("hello.downstreamUri", u);
                return serializeProperties(props);
            });
        } else {
            return Output.of(serializeProperties(props));
        }
    }

    private static String serializeProperties(Properties properties) {
        var writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // remove the date from serialized properties
        return writer.toString().replaceFirst("^#.+\n", "");
    }

    public ContainerApp(String name, Namespace ns, String hostName, String contextRoot) {
        this(name, ns, hostName, contextRoot, null);
    }

    public ContainerApp(String name, Namespace ns, String hostName, String contextRoot, Output<String> downstream) {
        this(name, new ContainerAppArgs(ns.metadata().applyValue(m -> m.name().orElseThrow()), hostName, contextRoot, downstream), null);
    }

}
