DELETE FROM System_Message_Type
WHERE system_message_type_id IN (
    'PosCashOrderItemsFeed',
    'NetSuiteOrderItemsFeed',
    'ReqFieldMisPosCashOrder',
    'ReqFieldMisOrderItems'
);
DELETE FROM Enumeration
WHERE enum_id IN (
    'PosCashOrderItemsFeed',
    'NetSuiteOrderItemsFeed',
    'ReqFieldMisPosCashOrder',
    'ReqFieldMisOrderItems'
);