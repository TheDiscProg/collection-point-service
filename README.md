# Collection Point Service
This service serves as a colletion point service within a system using the Event Driven Architecture with SIMEX (EDA/SIMEX)

A client makes a request by posting a SIMEX message at the drop-off service API.

Rather than waiting for a response, as some backend processes can take a long time, the client will make an application
defined periodic call to the collection point service to see if the response is ready.

If the response is not in the cache, the service will return **HTTP status code 204 - Not Content** (as this is a valid request).

If a matching response is found in the cache, it will return the response with **HTTP status code 200** after security checks.

## How the response is matched to the request
The original request had the following defined by the client in the message's `originator` section:
1. *Client ID* - the client ID of the system originating the request
2. *Request ID* - the request ID of the system originating the request
3. *originalToken* - the *authorization* token to make the request

When a message with the `method` defined as `RESPONSE`, it will cache it using format of 
 `${clientId}-${requestId}`

## Security in SIMEX Message
There are three levels of security enforced within SIMEX message:

1. If both the client ID and request ID are suitably random and unique, then these two values can be used for basic security
2. A valid and current authorization token must be presented
3. The `originalToken` must be presented and matched

These three levels build on top of each other, in other words, the highest level, 3, must include:

1. The correct client and request ID
2. A valid authorisation token
3. The original `originalToken`

The `originalToken` can be the same as the `authorization` token, but it doesn't have to be. For level 3, whatever is used must match.

## Enabling Security in Collection Point Service
There are two ways to enable security within collection point service:

1. Using the `security` attribute in `originator` section. This can be *user defined* but commonly is one of `Level 1`, `Level 2`, or `Level 3`
2. Defining the security level for the `entity` attribute within the service, thus over-riding the security level in the SIMEX message

When the `security` attribute is used, then the originator defines the security level of the response. However, in some cases,
it makes sense to override this using the `entity` attribute checks within the collection point service.

It should be noted that although the above security is defined within EDA/SIMEX as the default, the structure of SIMEX messages is such that
it can be extended or a different security system can be used.

### Security for Authorisation and Refresh Token
The security around authentication and refreshing tokens has to be based on client ID and request ID, as there is a possibility
that the client's authorization token has expired, and, therefore, cannot be used. In these cases, it is extremely important
that the request ID is suitably long and random. One good option is for the client to generate it's own JWT token for request ID
and to check that this is returned in the request ID.

## Incoming SIMEX Messages

All SIMEX messages coming into the system via the RabbitMQ must have the following conditions for it to be stored in cache:

1. The `Endpoint` method must be `RESPONSE`
2. The security level for the message must be defined, either by `originator.security` attribute or by using the `entity` attribute

As mentioned above, the message is stored using the `originator.clientId`-`originator.requestId` as the key.

## Disabling Security
In order to make some responses available to all clients without any security, a predefined and fixed `clientId` and `requestId` 
can be used. By having a fixed client ID/request ID list that is shared across the clients and the backend services, it provides a certain
level of security without exposing the APIs.

## What's included
There is a simple health check API that returns a status.  
A basic health check is enabled but other health checks can be added (see below).  
Please note that the health check API does not use SIMEX.

## Extending the service for specific service
Use the ***base*** section for entities (basic data carriers) and interfaces or traits.  
The package ***simex.entities*** is where any SIMEX request/response should live. As these are evolved 
to a final solution, they can be moved into a separate repository.  
The package name should follow the format ***simex.\<application\>.domain.\<service\>**  

### Package Naming: Application
This should refer to the name of the service or application, for example, ***authenticator***.

### Package Naming: Service
Within an application, there are different ***domains*** of concern. Here are some examples:
* Database access: ***repository***
* External services: the service should be named by which external service it is accessing 
* Messaging systems: Such as RabbitMQ, hence can be called ***rabbit***

Within the service package, sub-packages can be created for entities, services, etc, in order to 
make the code cleaner. However, entities and traits should all live in ***base***.

## Health Check
A basic health check is enabled in this service, but additional health checks can be enabled as described below. An 
high-level description is given below.

HeathRoutes --> HealthCheckService --> List: HealthChecker

**HealthCheckService** returns ***HealthCheckStatus*** which consists of:  
***HealthStatus***: The overall status of the service  
A list of ***HealthCheckerResponse*** each containing the response of a **HealthChecker**.

### Create Specific Health Check
Create a specific checker by implementing the trait **HealthChecker**.

### Add Checker to Checkers list for HealthCheckService
Add the health checker to the list for **HealthCheckService** in **AuthenticatorServer**.