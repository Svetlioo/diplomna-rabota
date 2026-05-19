output "resource_group_name" {
  description = "Name of the AKS resource group."
  value       = azurerm_resource_group.aks.name
}

output "cluster_name" {
  description = "Name of the AKS cluster."
  value       = azurerm_kubernetes_cluster.main.name
}

output "kube_config" {
  description = "Raw kubeconfig for the AKS cluster. Sensitive."
  value       = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive   = true
}
