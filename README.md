# It (only) starts with containers

This repository contains sample code for the talk "It (only) starts with containers" which demonstrates various approaches to deploying a Java application in Kubernetes.

**WARNING**: These yaml files are of "works on my cluster" kind. You will need to modify your domain and ingress settings to something that works in your environment.

## The Application

The sample application is a Jakarta EE REST service built with:
- **Java 21** and **Jakarta EE 10**
- **Payara Micro** as the runtime
- **MicroProfile Config** for configuration
- Simple REST endpoints for demonstration

### Features
- `GET /hello` - Returns a greeting message
- Configurable default name via `hello.defaultName` property
- Optional downstream service calls to demonstrate service-to-service communication
- Hostname detection for tracing requests across instances

## Deployment Approaches

This repository demonstrates 6 different approaches to deploy the same Java application to Kubernetes:

### 1. Plain YAML (`01-yaml/`)
Direct Kubernetes manifests with all resources defined explicitly.
- `instance-1.yaml` - Complete deployment with Namespace, ConfigMap, Deployment, Service, and Ingress
- `instance-2.yaml` - Second instance with different configuration

### 2. Kustomize (`02-kustomize/`)
Template-free customization of Kubernetes objects using overlays.
- `base/` - Base manifests
- `first-instance/`, `second-instance/`, `third-instance/` - Environment-specific overlays
- `both-instances/` - Deploy multiple instances together -- doesn't work. Need to be sorted with ArgoCD

### 3. Helm (`03-helm/`)
Package manager for Kubernetes with templating capabilities.
- `container-app/` - Helm chart with templates
- `*-values.yaml` - Environment-specific value files
- `combined/` - Multi-instance deployment using same base chart

### 4. Pulumi (`04-pulumi/`)
Infrastructure as Code using familiar programming languages.
- `container-app/` - Pulumi project for deployment automation in Java

### 5. ArgoCD (`05-argocd/`)
GitOps continuous delivery tool for Kubernetes.
- Application definitions for managing deployments
- `project.yml` - ArgoCD project configuration
- References to the other deployment approaches

### 6. Domain Keep-Alive (`06-domain-keepalive/`)
Utility deployment to keep demo domains active.

## Getting Started

### Building the Application
```bash
cd container-app
mvn clean package
```

### Running Locally
```bash
mvn clean package payara-micro:dev
```

### Building Container Image
```bash
mvn package docker:push # change the image name you hopefully cannot push under my creds in Docker Hub
# Image: pdudits/container-talk-demo:latest
```

### Deploying to Kubernetes

Choose your preferred deployment method:

**Plain YAML:**
```bash
kubectl apply -f 01-yaml/instance-1.yaml
```

**Kustomize:**
```bash
kubectl apply -k 02-kustomize/first-instance/
```

**Helm:**
```bash
helm install first-instance 03-helm/container-app/ -f 03-helm/first-instance-values.yaml
```

**Pulumi:**
```bash
cd 04-pulumi/container-app
pulumi up
```

## Configuration

Each deployment approach demonstrates different configuration methods:
- Environment-specific default names
- Resource limits and requests
- Ingress configurations for external access
- Service-to-service communication setup

## Talk Objectives

This repository illustrates the evolution from simple container deployment to sophisticated GitOps workflows, showing:
1. The progression of tooling complexity
2. Trade-offs between simplicity and maintainability
3. How each approach addresses different operational needs
4. Best practices for production deployments

The talk emphasizes that while containers are the starting point, production-ready deployments require thoughtful consideration of deployment strategies, configuration management, and operational workflows.