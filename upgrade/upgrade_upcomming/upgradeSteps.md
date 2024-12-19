## Upgrade Steps
1. Generic Steps for Upgrade data
    1. Update the instance with the data load command to load the upgrade data only.
    2. Follow the client specific manual if any.
    3. Check the Nifi flows if configured.


### Service job.
1. New service job is created to create the orders from hotwax to netsuite.
2. import the job data and configure the required parameters in the service job.

### System message type
1. New system message type and system message type parameter is added.
- NetSuiteOrderItemsFeed 
- NetSuitePOSOrderItemsFeed
2. configure the send path in the system message type as per the required directory path.
