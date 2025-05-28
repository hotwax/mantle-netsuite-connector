<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
.condition("orderId", fulfilledOrderItem.orderId)
.condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
.list()! />
<#assign externalOrderId = fulfilledOrderItem.orderId>

<#if orderIdentifications?has_content>
<#assign externalOrderId = orderIdentifications[0].get("idValue")!fulfilledOrderItem.orderId>
</#if>

{
"order_id": "${externalOrderId}",
"maySplit": "${fulfilledOrderItem.maySplit!''}",
"items": [
<#list allOrderItems as item>
{
"line_id": "${item.orderItemSeqId}",
"shipment_method_type_id": "${item.slaShipmentMethodTypeId!''}",
"quantity": "${item.quantity!1}",
"location_id": "${item.facilityExternalId!''}",
"tags": "hotwax-fulfilled"
}<#if item_has_next>,</#if>
</#list>
]
}
