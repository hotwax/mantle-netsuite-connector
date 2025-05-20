<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
.condition("orderId", fulfilledOrderItem.orderId)
.condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
.list()!>

<#-- Default to the internal orderId -->
<#assign externalOrderId = fulfilledOrderItem.orderId>

<#if orderIdentifications?has_content>
<#assign externalOrderId = orderIdentifications[0].get("idValue")!fulfilledOrderItem.orderId>
</#if>

{
"order_id": "${externalOrderId?json_string}",
"maySplit": "${fulfilledOrderItem.maySplit?default('N')?json_string}",
"items": [
{
"line_id": "${fulfilledOrderItem.orderItemSeqId?json_string}",
"shipment_method_type_id": "${fulfilledOrderItem.slaShipmentMethodTypeId?json_string}",
"quantity": "${fulfilledOrderItem.quantity?string?json_string}",
"location_id": "${fulfilledOrderItem.facilityExternalId?json_string}",
"tags": "hotwax-fulfilled"
}
]
}
