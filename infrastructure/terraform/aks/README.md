# aks

Provisions the Azure Kubernetes Service cluster used to host all diploma workloads.

## Resources

- `azurerm_resource_group` — `rg-diploma-aks`
- `azurerm_kubernetes_cluster` — single-node AKS cluster with `kubenet` networking and a system-assigned managed identity. Control plane is free; nodes are billed as regular VMs.

## State backend

Remote state in the shared storage account created by `infrastructure/terraform/shared/`:
- Storage account: `stdiplomarabotastate26`
- Container: `tfstate`
- Key: `aks.tfstate`

## Prerequisites

Register the AKS resource provider once per subscription before the first apply:

```bash
az provider register --namespace Microsoft.ContainerService --wait
```

## Apply

```bash
cd infrastructure/terraform/aks

terraform init
terraform plan
terraform apply
```

Apply takes ~5–10 minutes for AKS provisioning.

## kubectl access

After apply:

```bash
az aks get-credentials --resource-group rg-diploma-aks --name aks-diploma --overwrite-existing
kubectl get nodes
```

## Cost control

The AKS control plane is free. The single node (Standard_B2s) costs ~$30/month at full uptime.

Stop the cluster when not in use to avoid charges:

```bash
az aks stop --resource-group rg-diploma-aks --name aks-diploma
az aks start --resource-group rg-diploma-aks --name aks-diploma
```

## Inputs

| Name | Description | Default |
|---|---|---|
| `subscription_id` | Azure subscription ID | — |
| `location` | Azure region | `polandcentral` |
| `resource_group_name` | Name of the AKS resource group | `rg-diploma-aks` |
| `cluster_name` | Name of the AKS cluster | `aks-diploma` |
| `node_size` | VM size for the default node pool | `Standard_B2s` |
| `node_count` | Number of nodes in the default node pool | `1` |
| `kubernetes_version` | Kubernetes version | `1.31` |

## Outputs

| Name | Description |
|---|---|
| `resource_group_name` | Name of the AKS resource group |
| `cluster_name` | Name of the AKS cluster |
| `kube_config` | Raw kubeconfig (sensitive) |
