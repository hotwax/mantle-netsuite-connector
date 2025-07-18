<#-- Fetching address related to the item -->
<#assign shippingAddress = ec.entity.find("co.hotwax.financial.PostalAddressView")
    .condition("contactMechId", brokerOrderItem.postalContactMechId)
    .useCache(true)
    .one()!>

<#assign netsuiteProductId = Static["co.hotwax.netsuite.NetSuiteMappingWorker"].getProductId(ec, brokerOrderItem.productId)!>
<#assign netsuiteOrderId = Static["co.hotwax.netsuite.NetSuiteMappingWorker"].getOrderId(ec, brokerOrderItem.orderId)!>
<#assign netsuiteShipmentMethod = Static["co.hotwax.netsuite.NetSuiteMappingWorker"].getIntegrationTypeMappingValue(ec,'NETSUITE_SHP_MTHD', brokerOrderItem.shipmentMethodTypeId)!>

[{
  "lineId": "${brokerOrderItem.netsuiteItemLineId!''}",
  <#if netsuiteOrderId?has_content> "internalId": "${netsuiteOrderId!}", </#if>
  <#if netsuiteProductId?has_content> "item": "${netsuiteProductId!}", </#if>
  "closed": "",
  "quantity": "${brokerOrderItem.orderItemQuantity!''}",
  "location": "${brokerOrderItem.facilityExternalId!''}",
  <#if shippingAddress?has_content>
      "addressee": "${shippingAddress.toName!''}",
      "address1": "${shippingAddress.address1!''}",
      "address2": "${shippingAddress.address2!''}",
      "city": "${shippingAddress.city!''}",
      "state": "${shippingAddress.stateProvinceGeoName!''}",
      "country": "${shippingAddress.countryGeoCode!''}",
      "zip": "${shippingAddress.postalCode!''}",
  </#if>
  "shippingMethod": <#if netsuiteShipmentMethod?has_content>"${netsuiteShipmentMethod!''}"<#else>"${brokerOrderItem.shipmentMethodTypeId!''}" </#if>
}]
