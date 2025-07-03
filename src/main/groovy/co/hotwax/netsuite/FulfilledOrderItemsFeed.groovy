package co.hotwax.netsuite

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import org.moqui.entity.EntityValue


EntityValue systemMessageType = ec.entity.find("moqui.service.message.SystemMessageType")
    .condition("systemMessageTypeId", systemMessageTypeId).one()

if (systemMessageType == null) {
    ec.message.addError("Could not find SystemMessageType with ID ${systemMessageTypeId}")
    return;
}

def fulfilledOrdersItems = ec.entity.find("co.hotwax.netsuite.warehouse.FulfilledOrderItems")
if (orderId) fulfilledOrdersItems.condition("orderId", orderId)
if (orderItemSeqId) fulfilledOrdersItems.condition("orderItemSeqId", orderItemSeqId)

//If no eligible fulfilled orders, then don't generate the file
long fulfilledOrdersCount = fulfilledOrdersItems.count()
if (fulfilledOrdersCount == 0) {
    ec.message.addMessage("No eligible orders for Fulfilled Order and Items Feed at ${ec.user.nowTimestamp}, not generating the HotWax Feed file.")
    return;
}

EntityValue ftlFileResource = ec.entity.find("moqui.service.message.SystemMessageTypeParameter")
    .condition("systemMessageTypeId", systemMessageTypeId).condition("parameterName", "resourcePath").one()

if (ftlFileResource && ftlFileResource.parameterValue) {
    templateLocation = (ftlFileResource.parameterValue)
}

Boolean isFirstFile = true
int fileCount = 1
int totalFileCount = (fulfilledOrdersCount + fulfilledOrdersCountPerFeed - 1) / fulfilledOrdersCountPerFeed
def csvHeaders = ['lineId','internalId','item','quantity','location','tags','closed','addressee','address1','address2','city','state','country','zip','shippingMethod']
def createdSystemMessageIds = []

try (fulfilledOrdersItemsItr = fulfilledOrdersItems.iterator()) {
    while (fileCount <= totalFileCount) {
        csvFilePathRef = (ec.resource.expand(systemMessageType.receivePath, null,[contentRoot: ec.user.getPreference('mantle.content.root') ?: 'dbresource://datamanager', date:ec.l10n.format(ec.user.nowTimestamp, 'yyyy-MM-dd'), dateTime:ec.l10n.format(ec.user.nowTimestamp, 'yyyy-MM-dd-HH-mm-ss-SSS'),                             productStoreId:productStoreId], false))
        csvFilePath = (ec.resource.getLocationReference(csvFilePathRef).getUri().getPath())
        File csvFile = new File(csvFilePath)
        if(!csvFile.parentFile.exists()) csvFile.parentFile.mkdirs()
        CSVFormat format = CSVFormat.DEFAULT.withQuote(null).withRecordSeparator("\n").withIgnoreEmptyLines();

        try (writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8); csvPrinter = new CSVPrinter(writer, format)) {
            csvPrinter.printRecord(csvHeaders)
            int fulfilledOrderItemCount = 0
            while (fulfilledOrdersItemsItr.hasNext() && fulfilledOrderItemCount < fulfilledOrdersCountPerFeed) {
                def fulfilledOrderItem = fulfilledOrdersItemsItr.next()
                fulfilledOrderItemCount++

                if (ftlFileResource && ftlFileResource.parameterValue) {
                    def templateWriter = new StringWriter()
                    ec.resourceFacade.template(templateLocation, templateWriter)
                    csvPrinter.printRecord(templateWriter)
                } else {
                    def fulfilledOrderItemMap = ['lineId': fulfilledOrderItem.netsuiteItemLineId ?: '', 'internalId': fulfilledOrderItem.netsuiteOrderId ?: '', 'quantity': fulfilledOrderItem.quantity ?: '1', 'location': fulfilledOrderItem.facilityExternalId ?: '', 'tags': 'hotwax-fulfilled' ]
                    csvPrinter.printRecord(fulfilledOrderItemMap.values().collect { it ?: "" })
                }
                ec.service.sync().name("create#co.hotwax.integration.order.OrderFulfillmentHistory")
                    .parameters([orderId: fulfilledOrderItem.orderId, orderItemSeqId: fulfilledOrderItem.orderItemSeqId, comments: 'Order Item sent as part of OMS to NetSuite Fulfilled Items Feed', createdDate: nowDate, externalFulfillmentId: '_NA_'])
                    .call()
                if (fulfilledOrderItemCount >= fulfilledOrdersCountPerFeed || !fulfilledOrdersItemsItr.hasNext()) {
                    csvPrinter.flush()
                    writer.close()

                    def fulfillmentFeedSysMsgOut = ec.service.sync().name("org.moqui.impl.SystemMessageServices.queue#SystemMessage")
                        .parameters([systemMessageTypeId: systemMessageTypeId, systemMessageRemoteId: systemMessageRemoteId, messageText: csvFilePath])
                        .call()
                    createdSystemMessageIds.add(fulfillmentFeedSysMsgOut.systemMessageId)

                    fileCount = fileCount + 1
                    break
                }
            }
        } catch (Exception e) {
            throw ("Error preparing fulfilled order feed file ${e}")
        }
    }
} catch (Exception e) {
    throw ("Error preparing fulfilled order feed file ${e}")
}

ec.message.addMessage("Completed Fulfilled Order Items Feed file with ${totalFileCount} at time ${ec.user.nowTimestamp} with type ${systemMessageTypeId} and remote ${systemMessageRemoteId} saved response in messages ${createdSystemMessageIds}")
return;