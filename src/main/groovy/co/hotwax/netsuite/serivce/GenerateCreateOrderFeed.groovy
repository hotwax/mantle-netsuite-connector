import static org.moqui.util.ObjectUtilities.*
import static org.moqui.util.CollectionUtilities.*
import static org.moqui.util.StringUtilities.*
import java.sql.Timestamp
import java.sql.Time
import java.time.*
import org.moqui.entity.EntityValue
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import org.moqui.entity.EntityCondition
import co.hotwax.common.SchemaValidation


nowTimestamp = ec.user.nowTimestamp
ec.logger.info("Generating Order Feed file for Order ${orderId} at time ${nowTimestamp}")

EntityValue systemMessageType = ec.entity.find("moqui.service.message.SystemMessageType")
        .condition("systemMessageTypeId", systemMessageTypeId).one()
if (systemMessageType == null) {
    ec.logger.error("Could not find SystemMessageType with ID ${systemMessageTypeId}")
    return;
}

netsuiteOrders_find = ec.entity.find("co.hotwax.order.EligibleOrdersForNetSuiteView")
if(orderId) netsuiteOrders_find.condition("orderId", orderId)
if(fromOrderDate) netsuiteOrders_find.condition("orderDate", EntityCondition.ComparisonOperator.GREATER_THAN, fromOrderDate)
if(thruOrderDate) netsuiteOrders_find.condition("orderDate", EntityCondition.ComparisonOperator.LESS_THAN, fromOrderDate)
if(includeSalesChannel) netsuiteOrders_find.condition("salesChannelEnumId",EntityCondition.ComparisonOperator.IN, includeSalesChannel)
if(excludeSalesChannel) netsuiteOrders_find.condition("salesChannelEnumId",EntityCondition.ComparisonOperator.NOT_IN, excludeSalesChannel)

// Add conditions based on isCashOrderFeed parameter
if (isCashOrderFeed == "Y") {
    netsuiteOrders_find.condition("nonPosCompletedItemsCount", EntityCondition.ComparisonOperator.EQUALS, 0)
} else if (isCashOrderFeed == "N") {
    netsuiteOrders_find.condition("nonPosCompletedItemsCount", EntityCondition.ComparisonOperator.GREATER_THAN, 0)
}

netsuiteOrders_find.selectField("orderId,partyId,isMixCartOrder")
netsuiteOrders_find.distinct(true)

//If no eligible orders, then don't generate the file
long netsuiteOrdersCount = netsuiteOrders_find.count()
if (netsuiteOrdersCount == 0) {
    ec.message.addMessage("No eligible orders at ${nowTimestamp}, not generating the HotWax Feed file for Netsuite.")
    return
}

EntityValue enumValue = ec.entity.find("moqui.basic.Enumeration")
        .condition("enumId", systemMessageTypeId).one()
ec.logger.info("Found enumValue to produce for ${enumValue}")
EntityValue relatedSystemMessageType = null

if (enumValue && enumValue.relatedEnumId) {
    relatedSystemMessageType = ec.entity.find("moqui.service.message.SystemMessageType")
            .condition("systemMessageTypeId", enumValue.relatedEnumId).one()
    if (!relatedSystemMessageType) {
        ec.logger.warn("Could not find SystemMessageType with ID ${enumValue.relatedEnumId}, not producing requiredFieldsMissing system message.")
    }
} else {
    ec.logger.warn("Related SystemMessageType to produce for ${systemMessage.systemMessageTypeId} not defined, not producing requiredFieldsMissing system message.")
}

ec.logger.info("Found related SystemMessageType to produce for ${relatedSystemMessageType}")

csvFilePathRef = ec.resource.expand(systemMessageType.receivePath, null, [contentRoot: ec.user.getPreference('mantle.content.root') ?: 'dbresource://datamanager', date:ec.l10n.format(nowTimestamp, 'yyyy-MM-dd'), dateTime:ec.l10n.format(nowTimestamp, 'yyyy-MM-dd-HH-mm-ss-SSS')], false)
csvFilePath = ec.resource.getLocationReference(csvFilePathRef).getUri().getPath()

invalidFilePathRef = ec.resource.expand(relatedSystemMessageType.receivePath, null, [contentRoot: ec.user.getPreference('mantle.content.root') ?: 'dbresource://datamanager', date:ec.l10n.format(nowTimestamp, 'yyyy-MM-dd'), dateTime:ec.l10n.format(nowTimestamp, 'yyyy-MM-dd-HH-mm-ss-SSS')], false)
invalidFilePath = ec.resource.getLocationReference(invalidFilePathRef).getUri().getPath()


File csvFile = new File(csvFilePath)
File invalidFile = new File(invalidFilePath)
if (!csvFile.parentFile.exists()) csvFile.parentFile.mkdirs()
if (!invalidFile.parentFile.exists()) invalidFile.parentFile.mkdirs()
def isReqFieldMissFileEmpty = true
def isFileEmpty = true
def csvHeaders = null

EntityValue groovyFileResource = ec.entity.find("moqui.service.message.SystemMessageTypeParameter")
        .condition("systemMessageTypeId", systemMessageTypeId).condition("parameterName", "resourcePath").one()

if (groovyFileResource && groovyFileResource.parameterValue) {
    scriptPath = basicConvert(groovyFileResource.parameterValue, "String")
} else {
    scriptPath = ("component://mantle-netsuite-connector/template/GenerateOrderTransformation.ftl")
}

EntityValue schemaPath = ec.entity.find("moqui.service.message.SystemMessageTypeParameter")
        .condition("systemMessageTypeId", systemMessageTypeId).condition("parameterName", "schemaPath").one()

if (schemaPath) {
    schemaLocation = basicConvert(schemaPath.parameterValue, "String")
} else {
    schemaLocation = ("component://mantle-netsuite-connector/schemas/DefaultOrderSchema.json")
}

// Using try-with-resources to automatically close the EntityListIterator 'ordersItr'
try (
    def ordersItr = netsuiteOrders_find.iterator();
    def writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8);
    def invalidWriter = Files.newBufferedWriter(Paths.get(invalidFilePath), StandardCharsets.UTF_8);
    def csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    def invalidCsvPrinter = new CSVPrinter(invalidWriter, CSVFormat.DEFAULT)
    ) {
        int order_index = 0
        Iterator _orderIterator = ordersItr.iterator()
        // behave differently for EntityListIterator, avoid using hasNext()
        boolean orderIsEli = (_orderIterator instanceof org.moqui.entity.EntityListIterator)
        while (orderIsEli || _orderIterator.hasNext()) {
            order = _orderIterator.next()
            if (orderIsEli && order == null) break
            if (!orderIsEli) order_has_next = _orderIterator.hasNext()
                def result = ec.resource.script(scriptPath, null)

                def order = context.get("result")

                // Dynamically determine headers in the first iteration
                if (!csvHeaders) {
                    csvHeaders = order[0].keySet().collect { it as String }
                    csvPrinter.printRecord(csvHeaders)
                    invalidCsvPrinter.printRecord(csvHeaders)
                }

                try {
                    def schemaValidation = new co.hotwax.common.SchemaValidation(ec.ecfi ,ec)
                    schemaValidation.validateSchema(order, schemaLocation)
                    def orderedRecord = csvHeaders.collect { key -> order[key] != null ? order[key] : "" }
                    csvPrinter.printRecord(orderedRecord)
                    isFileEmpty = false

                } catch (Exception e) {
                    def orderedRecord = csvHeaders.collect { key -> order[key] != null ? order[key] : "" }
                    orderedRecord.add(e.message)
                    invalidCsvPrinter.printRecord(orderedRecord)
                    isReqFieldMissFileEmpty = false
                }


            order_index++
        }
        if(orderIsEli) _orderIterator.close()
    } catch (Exception e) {
        ec.message.addError("Error preparing order feed file ${e}")
    }

// Delete invalid file if empty
if (isReqFieldMissFileEmpty) {
    invalidFile.delete()
}
// end inline script
if (!isFileEmpty) {
    if (true) {
        def call_service_result = ec.service.sync().name("org.moqui.impl.SystemMessageServices.queue#SystemMessage")
                .parameters([systemMessageTypeId:systemMessageTypeId, systemMessageRemoteId:systemMessageRemoteId,                     messageText:csvFilePath]).call()
        if (FeedSysMsgOut != null) { if (call_service_result) FeedSysMsgOut.putAll(call_service_result) } else { FeedSysMsgOut = call_service_result }
        if (ec.message.hasError()) return
    }
}

if (!isReqFieldMissFileEmpty) {
    if (true) {
        def call_service_result = ec.service.sync().name("org.moqui.impl.SystemMessageServices.queue#SystemMessage")
                .parameters([systemMessageTypeId:relatedSystemMessageType.systemMessageTypeId, systemMessageRemoteId:systemMessageRemoteId,                     messageText:invalidFilePath]).call()
        if (MissingFieldSysMsgOut != null) { if (call_service_result) MissingFieldSysMsgOut.putAll(call_service_result) } else { MissingFieldSysMsgOut = call_service_result }
        if (ec.message.hasError()) return
    }
}

ec.message.addMessage("Generating Order Items Feed file with type ${systemMessageTypeId} and remote ${systemMessageRemoteId} saved response in messages ${FeedSysMsgOut?.systemMessageId} and missing file response in ${MissingFieldSysMsgOut?.systemMessageId})", "info")
return;
