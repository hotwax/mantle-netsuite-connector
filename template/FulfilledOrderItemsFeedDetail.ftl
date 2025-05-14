[
<#list brokerOrderItemList as brokerOrderItem>
  <#-- Check if 'netsuiteExported' is true -->
  <#assign netsuiteExportedAttr = ec.entity.find("OrderAttribute")
      .condition("orderId", brokerOrderItem.orderId)
      .condition("attrName", "netsuiteExported")
      .condition("attrValue", "Y")
      .useCache(true)
      .one()!>

  <#if netsuiteExportedAttr?has_content>

    <#-- Fetch product identification -->
    <#assign goodIdentificationList = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
        .condition("productId", brokerOrderItem.productId)
        .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
        .useCache(true)
        .list()!>

    <#-- Fetch order identifications -->
    <#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
        .condition("orderId", brokerOrderItem.orderId)
        .condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
        .list()!>

    <#-- Fetch shipping method mapping -->
    <#assign shippingMethodMapping = ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
        .condition("integrationTypeId", "NETSUITE_SHP_MTHD")
        .condition("mappingKey", brokerOrderItem.shipmentMethodTypeId)
        .list()!>

    <#-- Check if shipmentItem.internalName is PKG245 -->
    <#assign shipmentItem = ec.entity.find("ShipmentItem")
        .condition("orderId", brokerOrderItem.orderId)
        .condition("orderItemSeqId", brokerOrderItem.orderItemSeqId)
        .useCache(true)
        .one()!>

    <#assign orderItemExternalId = brokerOrderItem.orderId>
    <#if shipmentItem?has_content && shipmentItem.internalName == "PKG245">
        <#assign orderItemExternalId = "PKG245${brokerOrderItem.orderId}">
    </#if>

    {
      "order_id": "${brokerOrderItem.orderId}",
      "maySplit": "Y",
      "items": [
        {
          "line_id": "${brokerOrderItem.netsuiteItemLineId!''}",
          "shipment_method_type_id": <#if shippingMethodMapping?has_content>"${shippingMethodMapping[0].mappingValue!''}"<#else>"${brokerOrderItem.shipmentMethodTypeId!''}"</#if>,
          "quantity": "${brokerOrderItem.orderItemQuantity!''}",
          "location_id": "${brokerOrderItem.facilityExternalId!''}",
          "tags": "#{defaultTags}"
        }
      ]
    }
    <#if brokerOrderItem_has_next>,</#if>
  </#if>
</#list>
]
