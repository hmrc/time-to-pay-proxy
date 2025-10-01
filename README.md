# time-to-pay-proxy

This service acts as a facade to the  time to pay service, which is used to calculate and store time to pay plans for a customers' debt. This service exposes endpoints on the API platform to allow third party (private) access to the endpoints, and abstracts away the underlying implementation of time to pay.

# How to run it

The `time-to-pay-proxy` service can be run through the Service Manager via `DTD_ALL` profile:
```
sm --start DTD_ALL
```

Or locally with the testOnlyDoNotUseInAppConf.Routes specified: (port in service manager is 9600)
```
sbt "run <PORT>"
```

To run locally with Internal Auth enabled, a valid authentication token is needed when sending a request.
To generate a test-only token, ensure INTERNAL_AUTH is running and send a POST request to `http://localhost:8470/test-only/token`

The curl request below will generate an Authorization token, `valid-auth-token`, giving full permissions to access `time-to-pay-eligibility`:
```
curl -i -X POST -H 'Content-Type: application/json'  -d '{
  "token": "valid-auth-token",         
  "principal": "object-store",
  "permissions": [{
    "resourceType": "time-to-pay-eligibility",
    "resourceLocation": "*",
    "actions": ["*"]
  }]
}' 'http://localhost:8470/test-only/token'
```


The resources of this service are secured, which means that a bearer token should be provided [via auth login stub service](https://confluence.tools.tax.service.gov.uk/display/DTRG/Testing+an+API+microservice+locally).

This service can be run stand alone, although doesn't offer much value without time-to-pay service running.
