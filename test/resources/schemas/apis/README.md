# Test Resource Schemas

During the development of [DTD-3508](https://jira.tools.tax.service.gov.uk/browse/DTD-3508), 
the OpenAPI specs of the microservices that `time-to-pay-proxy` calls were split into Live and Proposed directories.
This was done to add validation tests for both the schemas and the models created in `time-to-pay-proxy` that are not yet ready for production,
while keeping the schemas and schema validation tests that are currently in production.

To access them in any schema validation tests, the path in test/uk/gov/hmrc/timetopayproxy/testutils/schematestutils/Validators.scala must be updated to include
`.../apis/live/<microservice-name>` or `.../apis/proposed/<microservice-name>`.

If you are working with an updated proposed schema that doesn't follow this pattern, please add them to their corresponding microservice directories [here](https://github.com/hmrc/time-to-pay-proxy/tree/main/test/resources/schemas/apis),
and create Live and Proposed objects within Validators.scala, where you can extract the schema components which have been updated. (See Validators.TimeToPayProxy.ChargeInfo in test/uk/gov/hmrc/timetopayproxy/testutils/schematestutils/Validators.scala as an example)