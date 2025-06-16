<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
    .condition("orderId", fulfilledOrderItem.orderId)
    .condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
    .list()! />

<#if orderIdentifications?has_content>
    <#assign externalOrderId = orderIdentifications[0].get("idValue")>
</#if>

{
    <#if externalOrderId?has_content>"order_id": "${externalOrderId}",</#if>
    "maySplit": "${fulfilledOrderItem[0].maySplit!''}",
    "items": [
        <#list fulfilledOrderItem as item>
            {
                "line_id": "${item.netsuiteItemLineId}",
                "shipment_method_type_id": "${item.slaShipmentMethodTypeId!''}",
                "quantity": "${item.quantity!1}",
                "location_id": "${item.facilityExternalId!''}",
                "tags": "hotwax-fulfilled"
            }<#sep>,</#sep>
        </#list>
    ]
}
