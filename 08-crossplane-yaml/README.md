# Crossplane Composition Demo

This directory demonstrates deploying container applications using Crossplane compositions, providing a declarative API comparable to the Pulumi component pattern in `04-pulumi`.

## Architecture

### XRD: Single App Instance
The `ContainerApp` XRD represents **one application instance** with minimal surface area:
- `hostName` (required): Ingress hostname
- `contextRoot` (optional, default `/`): Application path
- `config` (required): Application properties content
- `downstreamUri` (optional): For service chaining

Infrastructure decisions (image, replicas, ingress class, resources) are **baked into the Composition**, not exposed in the API.

### Composition: Patch & Transform
The `containerapp-pt` Composition uses `function-patch-and-transform` to render:
- **ConfigMap**: Application properties
- **Deployment**: Single replica, `pdudits/container-talk-demo:latest`, 250m CPU / 256Mi memory
- **Service**: ClusterIP on port 80
- **Ingress**: Traefik ingress class

### Example: Two Chained Instances
[20-instances.yaml](20-instances.yaml) mimics the Pulumi demo:
- `first-instance`: Root path `/`
- `second-instance`: Path `/second`, calls first instance via `downstreamUri`

## Files

| File | Purpose |
|------|---------|
| `00-xrd.yaml` | Composite Resource Definition for `ContainerApp` |
| `01-function.yaml` | Crossplane function package (`function-patch-and-transform`) |
| `10-composition-pt.yaml` | Patch & Transform composition implementation |
| `20-instances.yaml` | Two example app instances (first + second) |

## Deployment

Argo CD application [05-argocd/07-crossplane.yaml](../05-argocd/07-crossplane.yaml) syncs this folder and installs Crossplane.

After sync:
```bash
# Verify XRD and Composition
kubectl get xrd,composition

# Check instances
kubectl get containerapp -n demonstration-crossplane

# Verify rendered resources
kubectl get deploy,svc,ingress,cm -n demonstration-crossplane
```

## Future Work

- `11-composition-python.yaml`: Python function composition for side-by-side comparison
- Dynamic instance count / advanced logic scenarios
