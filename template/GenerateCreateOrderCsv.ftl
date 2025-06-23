<@compress>
    orderId,HCOrderId,orderDate,customer,salesChannel,addressee,address1,address2,city,state,zip,country,billingAddress1,billingAddress2,billingCity,billingState,billingZip,billingCountry,billingAddressee,phone,billingPhone,billingEmail,email,shippingCost,orderLineId,item,closed,shipmentMethod,itemLocation,location,orderType,department,subsidiary,priceLevel,quantity,orderNote,price,externalId,shopifyOrderNumber,HCShopifySalesOrderId,shippingTaxCode,taxCode,HCOrderTotal
    <#list ordersItr as netsuiteOrder>
        <#assign orderMaster = ec.entity.find("org.apache.ofbiz.order.order.OrderHeader").condition("orderId", netsuiteOrder.orderId).oneMaster("default")!>

        <#assign shippingEmail = "">
        <#assign billingEmail = "">
        <#assign shippingPhoneNo = "">
        <#assign billingPhoneNo = "">
        <#assign taxCode = "">
        <#assign shippingTaxCode = "">
        <#assign HCOrderId = "">
        <#assign HCShopifySalesOrderId = "">
        <#assign shopifyOrderNumber = "">
        <#assign externalId = "">

        <#-- Default empty contact mech object -->
        <#assign defaultContactMech = {
            "postalAddress": {
                "toName": "",
                "address1": "",
                "address2": "",
                "city": "",
                "stateProvinceGeoId": "",
                "postalCode": "",
                "countryGeoId": ""
            }
        }>

        <#if orderMaster.contactMechs?has_content>
            <#list orderMaster.contactMechs as orderContactMech>
                <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_LOCATION">
                    <#assign shippingContactMech = orderContactMech>
                </#if>
                <#if orderContactMech.contactMechPurposeTypeId == "BILLING_LOCATION">
                    <#assign billingContactMech = orderContactMech>
                </#if>
                <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_EMAIL" && orderContactMech.contactMech.infoString?has_content>

                    <#assign shippingEmail = orderContactMech.contactMech.infoString>
                </#if>
                <#if orderContactMech.contactMechPurposeTypeId == "ORDER_EMAIL" && orderContactMech.contactMech.infoString?has_content>
                    <#assign billingEmail = orderContactMech.contactMech.infoString>
                </#if>
                <#if orderContactMech.contactMechPurposeTypeId == "PHONE_BILLING">
                    <#assign billingPhone = orderContactMech>
                    <#assign billingPhoneNo = ((billingPhone.telecomNumber.countryCode!'') + '' + (billingPhone.telecomNumber.areaCode!'') + '' + (billingPhone.telecomNumber.contactNumber!'')?trim)>
                </#if>
                <#if orderContactMech.contactMechPurposeTypeId == "PHONE_SHIPPING">
                    <#assign shippingPhone = orderContactMech>
                    <#assign shippingPhoneNo = ((shippingPhone.telecomNumber.countryCode!'') + '' + (shippingPhone.telecomNumber.areaCode!'') + '' + (shippingPhone.telecomNumber.contactNumber!'')?trim)>
                </#if>
            </#list>
        </#if>

        <#-- Set default billing to shipping if not available -->
        <#if !billingContactMech?has_content && shippingContactMech?has_content>
            <#assign billingContactMech = shippingContactMech>
        </#if>

        <#-- Replace null contact mechs with empty objects -->
        <#if !shippingContactMech?has_content>
            <#assign shippingContactMech = defaultContactMech>
        </#if>
        <#if !billingContactMech?has_content>
            <#assign billingContactMech = defaultContactMech>
        </#if>

        <#-- Calculate total shipping cost from order adjustments -->
        <#assign shippingCost = 0>
        <#if orderMaster.adjustments?has_content>
            <#list orderMaster.adjustments as adjustment>
                <#if "SHIPPING_CHARGES" == adjustment.orderAdjustmentTypeId && adjustment.amount?has_content>
                    <#assign shippingCost = shippingCost + adjustment.amount>
                </#if>
            </#list>
        </#if>

        <#-- Add this after orderMaster is assigned to get order identifications -->
        <#if orderMaster.identifications?has_content>
            <#list orderMaster.identifications as orderId>
                <#if orderId.orderIdentificationTypeId == "SHOPIFY_ORD_ID">
                    <#assign HCShopifySalesOrderId = orderId.idValue!"">
                    <#assign externalId = orderId.idValue!"">
                <#elseif orderId.orderIdentificationTypeId == "SHOPIFY_ORD_NO">
                    <#assign shopifyOrderNumber = orderId.idValue!"">
                </#if>
            </#list>
        </#if>

        <#-- Get price level from order or customer -->
        <#assign priceLevel = "custom">

        <#-- CSV Data -->
        <#list orderMaster.shipGroups as shipGroup>
            <#if shipGroup?has_content>
                <#assign shippingMethodMapping =  ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
                    .condition("integrationTypeId","NETSUITE_SHP_MTHD")
                    .condition("mappingKey",shipGroup.shipmentMethodTypeId)
                    .list()!>

                <#-- Get department from facility -->
                <#assign department = "">

                <#if shipGroup.items?has_content>
                    <#list shipGroup.items as item>
                        <#assign orderDate = ec.l10n.format(orderMaster.orderDate, 'MM/dd/yyyy')>
                        <#assign goodIdentificationList = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
                            .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
                            .condition("productId", item.productId!)
                            .orderBy("-fromDate")
                            .list()
                            .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)>
                        <#assign netsuiteProductId = "">
                        <#if goodIdentificationList?has_content>
                            <#assign netsuiteProductId = goodIdentificationList[0].idValue!>
                        </#if>
                        <#assign isClosed = (item.statusId?upper_case == 'ITEM_CANCELLED')?string('true', 'false')>

                        <#-- Get location information -->
                        <#assign location = "">
                        <#if shipGroup.facilityId?has_content>
                            <#assign facilityType = ec.entity.find("co.hotwax.facility.FacilityAndType")
                                .condition("facilityId", shipGroup.facilityId)
                                .one()!>
                            <#if facilityType?has_content && facilityType.parentTypeId?has_content && facilityType.parentTypeId != "VIRTUAL_FACILITY">
                                <#assign location = facilityType.externalId!>
                            </#if>
                        </#if>
                        "${orderMaster.orderName!}","${orderMaster.orderId!}","${orderDate!}","${netsuiteOrder.netsuiteCustomerId!}","${netsuiteOrder.orderSalesChannelDescription!}","${shippingContactMech.postalAddress.toName!}","${shippingContactMech.postalAddress.address1!}","${shippingContactMech.postalAddress.address2!}","${shippingContactMech.postalAddress.city!}","${shippingContactMech.postalAddress.stateProvinceGeoId!}","${shippingContactMech.postalAddress.postalCode!}","${shippingContactMech.postalAddress.countryGeoId!}","${billingContactMech.postalAddress.address1!}","${billingContactMech.postalAddress.address2!}","${billingContactMech.postalAddress.city!}","${billingContactMech.postalAddress.stateProvinceGeoId!}","${billingContactMech.postalAddress.postalCode!}","${billingContactMech.postalAddress.countryGeoId!}","${billingContactMech.postalAddress.toName!}","${shippingPhoneNo!}","${billingPhoneNo!}","${billingEmail!}","${shippingEmail!}",${shippingCost},"${item.orderItemSeqId!}","${netsuiteProductId!}","${isClosed}","${shippingMethodMapping.mappingValue!}","${location}","","${netsuiteOrder.orderSalesChannelCode!}","${department}","${netsuiteOrder.productStoreExternalId!}","${priceLevel}",${item.quantity!1},"",${item.unitPrice!0},"${externalId}","${shopifyOrderNumber}","${HCShopifySalesOrderId}","${shippingTaxCode}","${taxCode}","${orderMaster.grandTotal!0}"
                    </#list>
                </#if>
            </#if>
        </#list>
    </#list>
</@compress>