output "resource_group_name" {
  description = "Name of the shared foundation resource group."
  value       = azurerm_resource_group.shared.name
}

output "state_storage_account_name" {
  description = "Name of the storage account holding Terraform state."
  value       = azurerm_storage_account.state.name
}

output "state_container_name" {
  description = "Name of the blob container holding Terraform state files."
  value       = azurerm_storage_container.tfstate.name
}
