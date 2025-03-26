# hdeu-access

The hdeu-access module is the bridge between hdeu-hub-ui and the backend services being executed.
Initially, the applications (data-access/data-request) are created and stored inside the mongo database
of hdeu-access. Once submitted, this information is sent to hdeu-dispatcher in order to be forwarded to the related
national contact points (NCPs). hdeu-access module is also updated with any status changes from the hdeu-dispatcher module.

## Table of Contents

***

1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Deployment](#deployment)
5. [API](#api)
6. [Security](#security)
7. [Known issues](#known-issues)

## Prerequisites

***
Install the following software

* MongoDB >= 4.0.28
* Java version = 17
* Vertx = 4.5.11

## Installation

***

Clone the directory and enter it

```bash
   $ git clone "repository/path/hdeu-access.git"
```
*Path needs to be replaced with the actual path

Build the project by using Maven and run the application. The generated _jar_ can be found in the `target` directory.

```bash
   $ mvn clean package
   $ java -jar target/hdeu-access.jar
```

As an alternative, you can also run the project in Docker:

1. Start your docker daemon
2. Build the application as described
3. Build the image: `docker build -t hdeu-access .`
3. Configure the environment variables as needed.
2. Make sure that MongDB is running
5. Run the image: `docker run -it -p 8080:8080 hdeu-access`

## Configuration

***
Edit the environment variables according to your local setup. Variables and their purpose are listed below:

| Key                                 | Description                                                                                                     | Example                                                                                                                                                                                                                                                                                                                                      | Type     |
|:------------------------------------|:----------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------|
| MONGO_DB_URI                        | The URL for the mongodb used by this service                                                                    | mongodb://mongodb:27017                                                                                                                                                                                                                                                                                                                      | `string` |
| MONGO_DB                            | The name for the database the applications will be stored in                                                    | mongodb                                                                                                                                                                                                                                                                                                                                      | `string` |
| MONGO_USER                          | The User for the database                                                                                       | ''                                                                                                                                                                                                                                                                                                                                           | `string` |
| MONGO_PW                            | The password for the database                                                                                   | ''                                                                                                                                                                                                                                                                                                                                           | `string` |
| HTTP_PORT                           | Port used to communicate with the API                                                                           | 8080                                                                                                                                                                                                                                                                                                                                         | `int`    |
| HUB_STORE_UPLOAD_DATA_URL           | The url of hdeu-hub-store for uploading files                                                                   | http://piveau-hub-simple-store:8080                                                                                                                                                                                                                                                                                                          | `string` |
| DATA_ACCESS_DISPATCHER_INTERVAL     | Timer in milliseconds for internal scheduler that sends the data-access applications to hdeu-dispatcher         | 300000                                                                                                                                                                                                                                                                                                                                       | `int`    |
| DATA_REQUEST_DISPATCHER_INTERVAL    | Timer in milliseconds for internal scheduler that sends the data-request applications to hdeu-dispatcher        | 300000                                                                                                                                                                                                                                                                                                                                       | `int`    |
| DATA_ACCESS_DISPATCHER_MAX_RETRIES  | The maximum number of retries for sending a data-access application towards hdeu-dispatcher in case that fails  | 5                                                                                                                                                                                                                                                                                                                                            | `int`    |
| DATA_REQUEST_DISPATCHER_MAX_RETRIES | The maximum number of retries for sending a data-request application towards hdeu-dispatcher in case that fails | 5                                                                                                                                                                                                                                                                                                                                            | `int`    |
| DISPATCHER_URL                      | The URL of hdeu-dispatcher for uploading files                                                                  | http://health-data-dispatcher:5100                                                                                                                                                                                                                                                                                                           | `string` |
| PIVEAU_STORE_API_KEY                | The api key of hdeu-hub-store                                                                                   | 0e3296cb-5106-4d77-9e9d-25ef9b6eac71                                                                                                                                                                                                                                                                                                         | `string` |
| HEALTH_DATA_ACCESS_API_KEY          | The api key of hdeu-access                                                                                      | c0107b96-78a1-408e-b05f-a65eb3a3d0a8                                                                                                                                                                                                                                                                                                         | `string` |
| DISPATCHER_X_API_KEY                | The api key of hdeu-dispatcher                                                                                  | 47173bdf-bba8-436c-912c-ad12be97ba34                                                                                                                                                                                                                                                                                                         | `string` |
| KEYCLOAK_INTROSPECT_PATH            | The path used for keycloak introspective                                                                        | /realms/healthdata/protocol/openid-connect/token/introspect                                                                                                                                                                                                                                                                                  | `string` |
| PIVEAU_ACCESS_AUTH_CONFIG           | The configuration required for hdeu-auth                                                                        | {"issuer": "\${KEYCLOAK_DOCKER_ADDRESS}/realms/healthdata","clientId":"eu-login-client","clientSecret":"HCOZDh8XP2cDBuyidDDnqdFrL2haM31s","customSecret":"","tokenServerConfig":{"keycloak":{"serverUrl":"${KEYCLOAK_DOCKER_ADDRESS}","realm":"healthdata","clientId":"eu-login-client","clientSecret":"HCOZDh8XP2cDBuyidDDnqdFrL2haM31s"}}} | `string` |
| HUB_SEARCH_DATA_URL                 | The URL of hdeu-hub-search                                                                                      | http://hub-search:8080                                                                                                                                                                                                                                                                                                                       | `string` |
| EXPORT_DATA_ACCESS_API_KEY          | the api key                                                                                                     | 0e3296cb-5106-4d77-9e9d-25ef9b6eac71gv                                                                                                                                                                                                                                                                                                       | `string` |

## Deployment

***
Deployment should be done using [Docker](https://www.docker.com/) containers.

## API

***
A formal OpenAPI 3 specification can be found in
the [/src/main/resources/webroot/openapi-data-permit.yml](/src/main/resources/webroot/openapi-data-permit.yml) file.

## Security

***
The service uses our Keycloak infrastructure for access control.

## Known issues

***
hdeu-access module is still under implementation. As a result much of the implementation has not yet reached its final form.

