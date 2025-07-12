<#-- Fetching address related to the item -->
<#assign shippingAddress = ec.entity.find("co.hotwax.financial.PostalAddressView")
    .condition("contactMechId", brokerOrderItem.postalContactMechId)
    .useCache(true)
    .one()!>

<#-- Fetching the Good Identification related to the product -->
<#assign goodIdentificationList = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
    .condition("productId", brokerOrderItem.productId)
    .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
    .useCache(true)
    .list()!>

<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
    .condition("orderId", brokerOrderItem.orderId)
    .condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
    .list()!>

<#assign shippingMethodMapping =  ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
    .condition("integrationTypeId","NETSUITE_SHP_MTHD")
    .condition("mappingKey",brokerOrderItem.shipmentMethodTypeId)
    .list()!>

{
  "lineId": "${brokerOrderItem.netsuiteItemLineId!''}",
  <#if orderIdentifications?has_content> "internalId": "${orderIdentifications[0].idValue!}", </#if>
  <#if goodIdentificationList?has_content> "item": "${goodIdentificationList[0].idValue!}", </#if>
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
  "shippingMethod": <#if shippingMethodMapping?has_content>"${shippingMethodMapping[0].mappingValue!''}"<#else>"${brokerOrderItem.shipmentMethodTypeId!''}" </#if>
}
