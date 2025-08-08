# time-to-pay-proxy

This service acts as a facade to the  time to pay service, which is used to calculate and store time to pay plans for a customers' debt. This service exposes endpoints on the API platform to allow third party (private) access to the endpoints, and abstracts away the underlying implementation of time to pay.

# How to run it

The `time-to-pay-proxy` service can be run through the Service Manager via `DTD_ALL` profile:
```
sm --start DTD_ALL
```

The resources of this service are secured, which means that a bearer token should be provided [via auth login stub service](https://confluence.tools.tax.service.gov.uk/display/DTRG/Testing+an+API+microservice+locally).

This service can be run stand alone, although doesn't offer much value without time-to-pay service running.

## OpenAPI Specification

The OpenAPI spec is maintained as split files in `resources/public/api/conf/1.0/openapi-split/` for better version control.

### Setup (macOS/Linux)
```bash
npm install -g @redocly/cli
```

### Combining the spec
```bash
./project/combine-openapi.sh
```

This generates `resources/public/api/conf/1.0/application.yaml` from the split files.
