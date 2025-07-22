-- Upgrade SQL to remove the not required GenerateOMSUpdateTOShipmentFeed SMT and enumeration
delete from enumeration where enum_id = 'GenerateOMSUpdateTOShipmentFeed';
delete from system_message_type where system_message_type_id = 'GenerateOMSUpdateTOShipmentFeed';

