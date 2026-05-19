terraform {
  backend "azurerm" {
    resource_group_name  = "rg-diploma-shared"
    storage_account_name = "stdiplomarabotastate26"
    container_name       = "tfstate"
    key                  = "aks.tfstate"
  }
}
