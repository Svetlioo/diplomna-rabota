# shared

Foundation Terraform module. Creates the resource group and the Azure Storage Account that holds remote Terraform state for all other modules in this repository.

## Resources

- `azurerm_resource_group` — holds shared foundation resources.
- `azurerm_storage_account` — Terraform remote state backend.
- `azurerm_storage_container` — blob container inside the storage account.

## Bootstrap

This module manages the remote state backend, so it cannot use it for itself. State for this module stays local on disk and is committed nowhere (`.terraform/` is gitignored).

```bash
cd infrastructure/terraform/shared

terraform init

terraform apply \
  -var subscription_id=<your-azure-subscription-id> \
  -var state_storage_account_name=<globally-unique-name>
```

After `apply`, other modules in this repository configure the `azurerm` backend pointing at the storage account created here.

## Inputs

| Name | Description | Default |
|---|---|---|
| `subscription_id` | Azure subscription ID | — |
| `location` | Azure region | `polandcentral` |
| `resource_group_name` | Name of the shared resource group | `rg-diploma-shared` |
| `state_storage_account_name` | Globally unique storage account name (3-24 lowercase alphanumeric chars) | — |
| `state_container_name` | Name of the state blob container | `tfstate` |

## Outputs

| Name | Description |
|---|---|
| `resource_group_name` | Name of the shared resource group |
| `state_storage_account_name` | Name of the state storage account |
| `state_container_name` | Name of the state blob container |
