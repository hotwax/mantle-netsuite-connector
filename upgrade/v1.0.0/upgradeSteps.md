## Upgrade Steps
1. Generic Steps for Upgrade data
    1. Update the instance with the data load command to load the upgrade data only.
    2. Follow the client specific manual if any.
    3. Check the Nifi flows if configured.

### Service job.
1. **generate_CreateOrderFeed**
   1. New service job is created to create the orders from hotwax to NetSuite.
   2. Import the job data and configure the required parameters in the service job.

### System message type
- PosCashOrderItemsFeed 
  - Import the data from the respective custom repository to add the groovy file in for create order feed.
  - Add the path of the groovy file in the system message type parameter.
- NetSuiteOrderItemsFeed
  - Import the data from the respective custom repository to add the groovy file in for create order feed.
  - Add the path of the groovy file in the system message type parameter.
