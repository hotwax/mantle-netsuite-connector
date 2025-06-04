<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
    .condition("orderId", fulfilledOrderItem.orderId)
    .condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
    .list()! />

<#if orderIdentifications?has_content>
    <#assign externalOrderId = orderIdentifications[0].get("idValue")>
</#if>

{
    <#if externalOrderId??>"order_id": "${externalOrderId}",</#if>
    "maySplit": "${allOrderItems[0].maySplit!''}",
    "items": [
        <#list allOrderItems as item>
            {
                "line_id": "${item.netsuiteItemLineId}",
                "shipment_method_type_id": null,
                "quantity": "${item.quantity!1}",
                "location_id": "${item.facilityExternalId!''}",
                "tags": "hotwax-fulfilled"
            }<#if item_has_next>,</#if>
        </#list>
    ]
}
