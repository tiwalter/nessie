# Configuration and authentication in Tools

When Nessie is integrated into a broader data processing environment, authentication settings need to be provided in
a way specific to the tool used.

## Common Nessie client configuration options

| Configuration option               | Mandatory / default | Meaning                                                                                                                                                                   | 
|------------------------------------|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nessie.uri`                       | Mandatory           | Nessie REST endpoint                                                                                                                                                      |
| `nessie.authentication.*`          | Recommended         | Authentication options, see [below](#authentication-settings)                                                                                                             |
| `nessie.ref`                       | Mandatory           | Name of the Nessie reference, usually `main`.                                                                                                                             |
| `nessie.ref.hash`                  | Optional            | Hash on `nessie.ref`, usually not specified.                                                                                                                              |
| `nessie.tracing`                   | Optional            | Boolean property to optionally enable tracing.                                                                                                                            |
| `nessie.transport.read-timeout`    | Optional            | Network level read timeout in milliseconds. When running with Java 11, this becomes a request timeout.                                                                    |
| `nessie.transport.connect-timeout` | Optional            | Network level connect timeout in milliseconds.                                                                                                                            |
| `nessie.http-redirects`            | Optional            | Optional, specify how redirects are handled. `NEVER`: Never redirect (default),`ALWAYS`: Always redirect, `NORMAL`: Always redirect, except from HTTPS URLs to HTTP URLs. |
| `nessie.ssl.cipher-suites`         | Optional            | Optional, specify the set of allowed SSL cipher suites.                                                                                                                   |
| `nessie.ssl.protocols`             | Optional            | Optional, specify the set of allowed SSL protocols.                                                                                                                       |
| `nessie.ssl.sni-hosts`             | Optional            | Optional, specify the set of allowed SNI hosts.                                                                                                                           |
| `nessie.ssl.sni-matcher`           | Optional            | Optional, specify a SNI matcher regular expression.                                                                                                                       |

### Java 11 connection pool options

The Java 11 HTTP client can be configured using Java system properties. Since Java's `HttpClient`
API does not support the configuration of these properties programmatically, Nessie cannot expose
those via its configuration mechanism.

| System property                     | Meaning                                                                                                          |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `jdk.httpclient.connectionPoolSize` | The size of the HTTP connection pool.Defaults to `0`, which means the number of connections is unlimited.        |
| `jdk.httpclient.keepalive.timeout`  | Number of seconds an idle HTTP connection will be kept alive. Defaults is `1200` seconds.                        |
| `jdk.httpclient.receiveBufferSize`  | Size of the network level receive buffer size. Defaults to `0`, which means the operating system defaults apply. |
| `jdk.httpclient.sendBufferSize`     | Size of the network level send buffer size. Defaults to `0`, which means the operating system defaults apply.    |

!!! note
    See Javadoc of `javax.net.ssl.SSLParameters` for valid options/values for the configuration
    parameters starting with `nessie.ssl.`.

!!! note
    See Javadoc of `org.projectnessie.client.NessieConfigConstants` as well.

!!! note
    In case you run into issues with Nessie's new HTTP client for Java 11 and newer, you can try
    to use the legacy `URLConnection` based HTTP client by setting the system property
    `nessie.client.force-url-connection-client` to `true`.

## Spark

When Nessie is used in Spark-based environments (with [Iceberg](./iceberg/index.md) 
the Nessie authentication settings are configured via Spark session properties (Replace `<catalog_name>` with the name of your catalog).

=== "Java"
    ``` java
    // local spark instance, assuming NONE authentication
    conf.set("spark.sql.catalog.<catalog_name>", "org.apache.iceberg.spark.SparkCatalog")
        .set("spark.sql.catalog.<catalog_name>.authentication.type", "NONE")
        .set(...);
    spark = SparkSession.builder()
                        .master("local[2]")
                        .config(conf)
                        .getOrCreate();
    ```
=== "Python"
    ``` python
    # local spark instance, assuming NONE authentication
    spark = SparkSession.builder \
            .config("spark.sql.catalog.<catalog_name>", "org.apache.iceberg.spark.SparkCatalog") \
            .config("spark.sql.catalog.<catalog_name>.authentication.type", "NONE") \
            .config(...) 
            .getOrCreate()
    ```

### Property Prefixes

The `spark.sql.catalog.<catalog_name>` prefix identifies properties for the Nessie catalog. The `<catalog_name>` part is just
the name of the catalog in this case (not to be confused with the Nessie project name).

Multiple Nessie catalogs can be configured in the same Spark environment, each with its own
set of configuration properties and its own property name prefix.

## Flink

When Nessie is used in Flink with [Iceberg](./iceberg/index.md), the Nessie authentication settings are configured when creating the Nessie catalog in Flink (Replace `<catalog_name>` with the name of your catalog):

```python
table_env.execute_sql(
        """CREATE CATALOG <catalog_name> WITH (
        'type'='iceberg',
        'catalog-impl'='org.apache.iceberg.nessie.NessieCatalog',
        'authentication.type'='NONE')""")
```

## Authentication Settings

The sections below discuss specific authentication settings. The property names are shown without
environment-specific prefixes for brevity. Nonetheless, in practice the property names should be
given appropriate prefixes (as in the examples above) for them to be recognized by the tools and
Nessie code.

The value of the `authentication.type` property can be one of the following:

* `NONE` (default)
* `BEARER`
* `OAUTH2`
* `AWS`

### Authentication Type `NONE`

For the Authentication Type `NONE` only the `authentication.type` property needs to be set.

This is also the default authentication type if nothing else is configured.

### Authentication Type `BEARER`

For the `BEARER` Authentication Type the `authentication.token` property should be set to a valid
[OpenID token](https://openid.net/specs/openid-connect-core-1_0.html).

This authentication type is recommended only when the issued access token has a lifespan large
enough to cover the duration of the entire Nessie client's session. Once the token is expired, the
Nessie client will not be able to refresh it and will have to be restarted, with a different token.
If the token needs to be refreshed periodically, then the `OAUTH2` authentication type should be
preferred to this one.

### Authentication Type `OAUTH2`

The `OAUTH2` Authentication Type is able to authenticate against an OAuth2 server and obtain a valid
access token. Only Bearer access tokens are currently supported. The access token is then used to
authenticate against Nessie. The client will automatically refresh the access token. This
authentication type is recommended when the access token has a lifespan shorter than the Nessie
client's session lifespan.

Note that the Nessie server must be configured to accept OAuth2 tokens from the same server. For
example, if the OAuth2 server is Keycloak, this can be done by defining the following properties in
the `application.properties` file of the Nessie server:

```properties
nessie.server.authentication.enabled=true
quarkus.oidc.auth-server-url=https://<keycloak-server>/realms/<realm-name>
```
The most important property is `authentication.oauth2.grant-type`, which defines the grant type to
use when authenticating against the OAuth2 server. Valid values are: 

* `client_credentials` : enables the [Client Credentials grant] (default);
* `password` : enables the [Resource Owner Password Credentials grant];
* `authorization_code` : enables the [Authorization Code grant];
* `device_code` : enables the [Device Authorization grant].

[Client Credentials grant]: https://datatracker.ietf.org/doc/html/rfc6749#section-4.4
[Resource Owner Password Credentials grant]: https://datatracker.ietf.org/doc/html/rfc6749#section-4.3
[Authorization Code grant]: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
[Device Authorization grant]: https://datatracker.ietf.org/doc/html/rfc8628

The full list of available properties is shown hereafter. Depending on the grant type, some of them 
must be provided:

* For the "client_credentials" grant type, at least the following properties must be provided:
  * `authentication.oauth2.issuer-url` or `authentication.oauth2.token-endpoint`
  * `authentication.oauth2.client-id`
  * `authentication.oauth2.client-secret`
* For the "password" grant type, at least the following properties must be provided:
  * `authentication.oauth2.issuer-url` or `authentication.oauth2.token-endpoint`
  * `authentication.oauth2.client-id`
  * `authentication.oauth2.client-secret`
  * `authentication.oauth2.username`
  * `authentication.oauth2.password`
* For the "authorization_code" grant type, at least the following properties must be provided:
  * `authentication.oauth2.issuer-url`, or both `authentication.oauth2.token-endpoint` 
    and `authentication.oauth2.auth-endpoint`
  * `authentication.oauth2.client-id`
  * `authentication.oauth2.client-secret`
  * `authentication.oauth2.auth-endpoint`
* For the "device_code" grant type, at least the following properties must be provided:
  * `authentication.oauth2.issuer-url`, or both `authentication.oauth2.token-endpoint` 
    and `authentication.oauth2.deivce-auth-endpoint`
  * `authentication.oauth2.client-id`
  * `authentication.oauth2.client-secret`
  * `authentication.oauth2.auth-endpoint`

Here are the available properties for the `OAUTH2` authentication type:

* `authentication.oauth2.issuer-url`: The root URL of the OpenID Connect identity issuer provider,
  which will be used for discovering supported endpoints and their locations. For Keycloak, this is 
  typically the realm URL: `https://<keycloak-server>/realms/<realm-name>`. Optional. Either this 
  property or the `authentication.oauth2.token-endpoint` property must be provided. Endpoint 
  discovery is performed using the OpenID Connect Discovery metadata published by the issuer. 
  See [OpenID Connect Discovery 1.0] for more information.

* `authentication.oauth2.token-endpoint`: the URL of the OAuth2 token endpoint. For Keycloak, this 
  is typically `https://<keycloak-server>/realms/<realm-name>/protocol/openid-connect/token`. 
  Optional. Either this property or the `authentication.oauth2.issuer-url` property must be 
  provided.

* `authentication.oauth2.auth-endpoint`: the URL of the OAuth2 auth endpoint. For Keycloak, this is 
  typically `https://<keycloak-server>/realms/<realm-name>/protocol/openid-connect/auth`. If using 
  the "authorization_code" grant type, either this property or the 
  `authentication.oauth2.issuer-url` property must be provided.

* `authentication.oauth2.device-auth-endpoint`: the URL of the OAuth2 device auth endpoint. For 
  Keycloak, this is typically 
  `https://<keycloak-server>/realms/<realm-name>/protocol/openid-connect/auth/device`. If using 
  the "device_code" grant type, either this property or the `authentication.oauth2.issuer-url` 
  property must be provided.

* `authentication.oauth2.client-id`: the client ID to use when authenticating against the OAuth2
  server. Required.

* `authentication.oauth2.client-secret`: the client secret to use when authenticating against the
  OAuth2 server. Required.

* `authentication.oauth2.username`: the username to use when authenticating against the OAuth2
  server. Required if using the "password" grant type.

* `authentication.oauth2.password`: the password to use when authenticating against the OAuth2
  server. Required if using the "password" grant type.

* `authentication.oauth2.default-access-token-lifespan`: the default access token lifespan; if the
  OAuth2 server returns an access token without specifying its expiration time, this value will be
  used. Optional, defaults to `PT1M` (1 minute). Must be a valid [ISO-8601 duration].

* `authentication.oauth2.default-refresh-token-lifespan`: the default refresh token lifespan;
  if the OAuth2 server returns a refresh token without specifying its expiration time, this value
  will be used. Optional, defaults to `PT30M` (30 minutes). Must be a valid [ISO-8601 duration].

* `authentication.oauth2.refresh-safety-window`: the refresh safety window to use; a new token will
  be fetched when the current token's remaining lifespan is less than this value. Optional, defaults
  to `PT10S` (10 seconds). Must be a valid [ISO-8601 duration].

* `authentication.oauth2.client-scopes`: space-separated list of scopes to include in each request
  to the OAuth2 server. Optional, defaults to empty (no scopes). The scope names will not be
  validated by the Nessie client; make sure they are valid according to [RFC 6749 Section 3.3].

* `authentication.oauth2.token-exchange-enabled`: if set to `true`, the Nessie client will attempt
  to exchange access tokens for refresh tokens whenever appropriate. This, however, can only work if
  the OAuth2 server supports token exchange. Optional, defaults to `true` (enabled). Note that
  recent versions of Keycloak support token exchange, but it is disabled by default. See [Using
  token exchange] for more information and how to enable this feature.

* `authentication.oauth2.preemptive-token-refresh-idle-timeout`: for how long the Nessie 
  client should keep the tokens fresh, if the client is not being actively used. Setting this value 
  too high may cause an excessive usage of network I/O and thread resources; conversely, when 
  setting it too low, if the client is used again, the calling thread may block if the tokens are 
  expired and need to be renewed synchronously. Optional, defaults to `PT30S` (30 seconds). Must be 
  a valid [ISO-8601 duration].

* `authentication.oauth2.background-thread-idle-timeout`: how long the Nessie client should 
  keep a background thread alive, if the client is not being actively used, or no token refreshes 
  are being executed. Setting this value too high will cause the background thread to keep running 
  even if the client is not used anymore, potentially leaking thread and memory resources; 
  conversely, setting it too low could cause the background thread to be restarted too often.
  Optional, defaults to `PT30S` (30 seconds). Must be a valid [ISO-8601 duration].

* `authentication.oauth2.auth-code-flow.web-port`: The port used for the embedded web server 
  that listens for the authorization code callback. This is only used if the grant type to use is 
  "authorization_code". Optional; if not present, a random port will be used.

* `authentication.oauth2.auth-code-flow.timeout`: How long the client should wait for the
  authorization code flow to complete. This is only used if the grant type to use is
  "authorization_code". Optional, defaults to `PT5M` (5 minutes). Must be a valid 
  [ISO-8601 duration].

* `authentication.oauth2.device-code-flow.timeout`: How long the client should wait for the
  device code flow to complete. This is only used if the grant type to use is
  "device_code". Optional, defaults to `PT5M` (5 minutes). Must be a valid [ISO-8601 duration].

* `authentication.oauth2.device-code-flow.poll-interval`: How often the client should poll the 
  device authorization endpoint for a token. This is only used if the grant type to use is
  "device_code". Optional, defaults to `PT5S` (5 seconds). Must be a valid [ISO-8601 duration].

[ISO-8601 duration]: https://en.wikipedia.org/wiki/ISO_8601#Durations
[RFC 6749 Section 3.3]: https://datatracker.ietf.org/doc/html/rfc6749#section-3.3
[Using token exchange]: https://www.keycloak.org/docs/latest/securing_apps/index.html#internal-token-to-internal-token-exchange
[OpenID Connect Discovery 1.0]: https://openid.net/specs/openid-connect-discovery-1_0.html

#### Which grant type to use?

The "client_credentials" grant type is the simplest one, but it requires the client to be granted
enough permissions to access the Nessie server on behalf of the user. This is not always possible,
and should be avoided if the resource owner (the user) is a human.

The "password" grant type is also simple, but it requires passing the user's password to the client,
which may not be acceptable in some cases for security reasons.

For real users trying to authenticate within a terminal session, such as a Spark shell, the
"authorization_code" grant type is recommended. It requires the user to authenticate in a browser
window, thus sparing the need to provide the user's password directly to the client. The user will 
be prompted to authenticate in a separate browser window, and the Nessie client will be notified 
when the authentication is complete.

If the terminal session is running remotely however, on inside an embedded device, then the
"authorization_code" grant type may not be suitable, as the browser and the terminal session must 
be running on the same machine. In this case, the "device_code" grant type is recommended. Similar 
to the "authorization_code" grant type, it requires the user to authenticate in a browser window, 
but it does not require the browser and the terminal session to be running on the same machine. The 
user will be prompted to authenticate in a local browser window, and the remote Nessie client will
poll the OAuth2 server for the authentication status, until the authentication is complete.

### Authentication Type `AWS`

For the `AWS` Authentication Type the `authentication.aws.region` property should be set to the
AWS region where the Nessie Server endpoint is located.

Additional AWS authentication configuration should be provided via standard AWS configuration files. 
