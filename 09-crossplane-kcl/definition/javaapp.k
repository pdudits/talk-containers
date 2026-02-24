oxr = option("params").oxr
params = oxr.spec.parameters

name = oxr.metadata.name
namespace = oxr.metadata.namespace

host_name = params.hostName
context_root = params.contextRoot or "/"
config = params.config

configmap = {
  apiVersion: "v1"
  kind: "ConfigMap"
  metadata: {
    annotations: {
      "crossplane.io/composition-resource-name": "configmap"
    }
    name: name
    namespace: namespace
  }
  data: {
    "application.properties": config
  }
}

deployment = {
  apiVersion: "apps/v1"
  kind: "Deployment"
  metadata: {
    annotations: {
      "crossplane.io/composition-resource-name": "deployment"
    }
    name: name
    namespace: namespace
  }
  spec: {
    replicas: 1
    selector: {
      matchLabels: {
        app: name
      }
    }
    template: {
      metadata: {
        labels: {
          app: name
        }
      }
      spec: {
        containers: [{
          name: "container-app"
          image: "pdudits/container-talk-demo:latest"
          imagePullPolicy: "Always"
          args: [
            "--deploymentDir",
            "/opt/payara/deployments",
            "--contextRoot",
            context_root,
            "--systemProperties",
            "/config/application.properties"
          ]
          ports: [{
            name: "http"
            containerPort: 8080
            protocol: "TCP"
          }]
          livenessProbe: {
            httpGet: {
              path: "/health/live"
              port: "http"
            }
          }
          readinessProbe: {
            httpGet: {
              path: "/health/ready"
              port: "http"
            }
          }
          volumeMounts: [{
            mountPath: "/config"
            name: "config"
          }]
          resources: {
            requests: {
              cpu: "250m"
              memory: "256Mi"
            }
          }
        }]
        volumes: [{
          name: "config"
          configMap: {
            name: name
            items: [{
              key: "application.properties"
              path: "application.properties"
            }]
          }
        }]
      }
    }
  }
}

service = {
  apiVersion: "v1"
  kind: "Service"
  metadata: {
    annotations: {
      "crossplane.io/composition-resource-name": "service"
    }
    name: name
    namespace: namespace
  }
  spec: {
    type: "ClusterIP"
    ports: [{
      name: "http"
      port: 80
      protocol: "TCP"
      targetPort: "http"
    }]
    selector: {
      app: name
    }
  }
}

ingress = {
  apiVersion: "networking.k8s.io/v1"
  kind: "Ingress"
  metadata: {
    annotations: {
      "crossplane.io/composition-resource-name": "ingress"
    }
    name: name
    namespace: namespace
  }
  spec: {
    ingressClassName: "traefik-cloud-head"
    rules: [{
      host: host_name
      http: {
        paths: [{
          path: context_root
          pathType: "Prefix"
          backend: {
            service: {
              name: name
              port: {
                number: 80
              }
            }
          }
        }]
      }
    }]
  }
}

items = [configmap, deployment, service, ingress]
