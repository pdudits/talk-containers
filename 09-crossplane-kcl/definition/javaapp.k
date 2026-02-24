oxr = option("params").oxr
params = oxr.spec.parameters
ocds = option("params").ocds

app_name = oxr.metadata.name
namespace = oxr.metadata.namespace

host_name = params.hostName
context_root = params.contextRoot or "/"
config = params.config

configmap = {
  apiVersion: "v1"
  kind: "ConfigMap"
  metadata: {
    annotations: {
      "krm.kcl.dev/composition-resource-name": "configmap"
    }
    name: app_name
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
      "krm.kcl.dev/composition-resource-name": "deployment"
    }
    name: app_name
    namespace: namespace
  }
  spec: {
    replicas: 1
    selector: {
      matchLabels: {
        app: app_name
      }
    }
    template: {
      metadata: {
        labels: {
          app: app_name
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
            name: app_name
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
      "krm.kcl.dev/composition-resource-name": "service"
    }
    name: app_name
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
      app: app_name
    }
  }
}

ingress = {
  apiVersion: "networking.k8s.io/v1"
  kind: "Ingress"
  metadata: {
    annotations: {
      "krm.kcl.dev/composition-resource-name": "ingress"
    }
    name: app_name
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
              name: app_name
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

_items_base = [configmap, deployment, service, ingress]

# Patch XR status with URL from Ingress host and context root
_dxr = {
  **oxr
}

# Build URL from ingress rule host and context root path
_path = context_root if context_root != "/" else ""
_dxr.status.url = "https://{host}{path}".format(host=host_name, path=_path)

items = _items_base + [_dxr]
