# Bruno API collection - Import guide
This guide provides instructions for importing and setting up the API collection on Bruno locally.
___

## Prerequisites
Before you begin, ensure you have:
* Bruno installed
* Repo cloned locally

## Open Collection in Bruno
* Launch the **Bruno** application
* Click **File → Open Collection**
* Navigate to the cloned project location
* Select `timeToPayProxyAPIBrunoCollection` folder from inside the `timeToPayProxyBrunoCollectionAndGlobal`
* Click **Open**

The collection will load automatically with all requests and folders visible in the left sidebar.

# How to run the services

The `time-to-pay-proxy` service can be run through the Service Manager via `DTD_ALL` profile:
```
sm2 --start DTD_ALL
```

Or locally with the testOnlyDoNotUseInAppConf.Routes specified: (Port is 9600)
```
sbt "run <PORT>"
```

## How to use the collection
```
Once the collection is loaded, set the Global in Environment on the top right corner importing Global.json from "timeToPayProxyBrunoCollectionAndGlobal" package.
To sent the requests you will need a bearer token.
To get the token there is a folder in the collection named bearerToken.
Get the token sending the "POST - bearerTokenRequest" request.
Copy the token from the response's Headers -> authorization without "Bearer " and past it in request -> Auth  -> Token after selecting Bearer Token from the dropdown and.
You can now send the request manually.
```

## Endpoints in time-to-pay-proxy
```
POST        /quote                                   
GET         /quote/:customerReference/:planId        
PUT         /quote/:customerReference/:planId        
POST        /quote/arrangement                       
POST        /self-serve/affordable-quotes                        
POST        /charge-info
POST        /cancel                                  
POST        /inform                                  
POST        /full-amend                              
POST        /charge-migration  
```