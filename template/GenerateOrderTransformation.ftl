<#assign orderMaster = ec.entity.find("org.apache.ofbiz.order.order.OrderHeader").condition("orderId", orderId).oneMaster("default")!>
<#list orderMaster.contactMechs as orderContactMech>
    <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_LOCATION">
        <#assign shippingContactMech = orderContactMech>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "BILLING_LOCATION">
        <#assign billingContactMech = orderContactMech>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_EMAIL">
        <#assign shippingEmail = orderContactMech.contactMech.infoString>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "ORDER_EMAIL">
        <#assign billingEmail = orderContactMech.contactMech.infoString>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "PHONE_BILLING">
        <#assign billingPhone = orderContactMech>
        <#assign billingPhoneNo = ((billingPhone.countryCode!'') + '' + (billingPhone.areaCode!'') + '' + (billingPhone.contactNumber!'')?trim)>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "PHONE_SHIPPING">
        <#assign shippingPhone = orderContactMech>
        <#assign shippingPhoneNo = ((shippingPhone.countryCode!'') + '' + (shippingPhone.areaCode!'') + '' + (shippingPhone.contactNumber!'')?trim)>
    </#if>
</#list>

<#-- Calculate total shipping cost from order adjustments -->
<#assign shippingCost = 0>
<#if orderMaster.adjustments?has_content>
    <#list orderMaster.adjustments as adjustment>
        <#if "SHIPPING_CHARGES" == adjustment.orderAdjustmentTypeId && adjustment.amount?has_content>
            <#assign shippingCost = shippingCost + adjustment.amount>
        </#if>
    </#list>
</#if>

[
<#list orderMaster.shipGroups as shipGroup>
    <#if shipGroup?has_content>
        <#list shipGroup.items as item>
            {
                "orderName": "${orderDetails.orderName!}",
                "orderId": "${orderDetails.orderId!}",
                <#assign orderDate = ec.l10n.format(orderDetails.orderDate, 'MM/dd/yyyy')>
                "date": "${orderDate!}",
                "customer": "${order.netsuiteCustomerId!}",
                "salesChannel": "${order.orderSalesChannelDescription!}",
                "addressee": "${shippingContactMech.addressee!}",
                "address1": "${shippingContactMech.address1!}",
                "address2": "${shippingContactMech.address2!}",
                "city": "${shippingContactMech.city!}",
                "state": "${shippingContactMech.stateProvinceGeoId!}",
                "zip": "${shippingContactMech.postalCode!}",
                "country": "${shippingContactMech.geoCodeAlpha2!}",
                <#if billingContactMech?has_content>
                    "billingAddress1": "${billingContactMech.address1!}",
                    "billingAddress2": "${billingContactMech.address2!}",
                    "billingCity": "${billingContactMech.city!}",
                    "billingState": "${billingContactMech.stateProvinceGeoId!}",
                    "billingZip": "${billingContactMech.postalCode!}",
                    "billingCountry": "${billingContactMech.geoCodeAlpha2!}",
                    "billingAddressee": "${billingContactMech.addressee!}",
                    <#else>
                    "billingAddress1": "${shippingContactMech.address1!}",
                    "billingAddress2": "${shippingContactMech.address2!}",
                    "billingCity": "${shippingContactMech.city!}",
                    "billingState": "${shippingContactMech.stateProvinceGeoId!}",
                    "billingZip": "${shippingContactMech.postalCode!}",
                    "billingCountry": "${shippingContactMech.geoCodeAlpha2!}",
                    "billingAddressee": "${shippingContactMech.toName!}",
                </#if>
                "shippingPhone": "${shippingPhoneNo!}",
                "billingPhone": "${billingPhoneNo!}",
                "billingEmail": "${billingEmail!}",
                "email": "${shippingEmail!}",
                "shippingCost": ${orderDetails.shippingCost},
                "orderLineId": "${item.orderItemSeqId!}",
                <#assign goodIdentificationList = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
                    .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
                    .condition("productId", item.productId!)
                    .orderBy("-fromDate")
                    .list()
                    .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)>
                "item": "<#if goodIdentificationList?has_content>${goodIdentificationList[0].idValue!}</#if>",
                "closed": ${(item.statusId?upper_case == 'ITEM_CANCELLED')?string('true', 'false')},
                "shipmentMethodTypeId": "${shipGroup.shipmentMethodTypeId!}",

                <#assign facilityType = ec.entity.find("co.hotwax.facility.FacilityAndType")
                    .condition("facilityId", shipGroup.facilityId!)
                    .one()!>
                <#if facilityType?has_content && facilityType.parentTypeId != "VIRTUAL_FACILITY">
                    <#assign location = facilityType.externalId!>
                </#if>
                "location": "${location!}"
            }<#sep>, </#sep>
]