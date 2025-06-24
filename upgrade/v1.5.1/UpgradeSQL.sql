-- Upgrade SQL to pause the Transfer Order v1 Jobs.
update service_job set paused = 'Y' where job_name IN ('HC_MR_ExportedStoreTOFulfillmentCSV', 'HC_MR_ExportedStoreTransferOrderCSV', 'HC_MR_ExportedWHTOFulfillmentCSV', 'HC_SC_ImportTOFulfillmentReceipts', 'HC_SC_ImportTOItemFulfillment');

-- Upgrade SQL for removing the Transfer Order v1 Jobs if required.
Run the service delete#TransferOrderV1Jobs added in MigrateTransferOrderServices.xml file.