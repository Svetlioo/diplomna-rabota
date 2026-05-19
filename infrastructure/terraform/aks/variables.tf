variable "subscription_id" {
  description = "Azure subscription ID."
  type        = string
}

variable "location" {
  description = "Azure region for the AKS cluster."
  type        = string
  default     = "polandcentral"
}

variable "resource_group_name" {
  description = "Name of the resource group that holds the AKS cluster."
  type        = string
  default     = "rg-diploma-aks"
}

variable "cluster_name" {
  description = "Name of the AKS cluster."
  type        = string
  default     = "aks-diploma"
}

variable "node_size" {
  description = "VM size for the default node pool."
  type        = string
  default     = "Standard_B2s_v2"
}

variable "node_count" {
  description = "Number of nodes in the default node pool."
  type        = number
  default     = 1
}

variable "kubernetes_version" {
  description = "Kubernetes version for the AKS cluster."
  type        = string
  default     = "1.35.4"
}
