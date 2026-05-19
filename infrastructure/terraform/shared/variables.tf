variable "subscription_id" {
  description = "Azure subscription ID."
  type        = string
}

variable "location" {
  description = "Azure region for all resources in the shared foundation."
  type        = string
  default     = "polandcentral"
}

variable "resource_group_name" {
  description = "Name of the resource group that holds shared foundation resources."
  type        = string
  default     = "rg-diploma-shared"
}

variable "state_storage_account_name" {
  description = "Globally unique name of the storage account used for Terraform remote state. 3-24 lowercase alphanumeric chars."
  type        = string
}

variable "state_container_name" {
  description = "Name of the blob container that holds Terraform state files."
  type        = string
  default     = "tfstate"
}
