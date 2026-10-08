# How to run

Requirements: Java 17 or later. Maven is not needed: the project includes the Maven wrapper.

## 1. Start the application

Every request needs a JWT signed with a shared secret of at least 32 bytes. The secret is read from the
`JWT_SECRET` environment variable and is not stored in the repository. Without a valid secret, the
application refuses to start and explains why.

Bash (Linux, macOS, Git Bash):

```bash
export JWT_SECRET='replace-with-a-random-secret-of-32-bytes-or-more'
./mvnw spring-boot:run
```

PowerShell:

```powershell
$env:JWT_SECRET = 'replace-with-a-random-secret-of-32-bytes-or-more'
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080/user/v1/user`.

## 2. Get a token

No endpoint issues tokens. A small generator in the test sources, `it.sara.demo.web.security.JwtTokenGenerator`,
prints a token signed with `JWT_SECRET`. The token is valid for one hour, has the issuer expected by the application
(`https://auth.high-card.local`) and both scopes (`users:read` and `users:write`).

- **From IntelliJ:** run `JwtTokenGenerator`, with the `JWT_SECRET` environment variable in the run configuration.
- **From a second terminal**, with the same `JWT_SECRET` set:

  Bash on Linux or macOS (on Git Bash for Windows, replace the `:` separators with `;`):

  ```bash
  ./mvnw -q test-compile dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/cp.txt
  TOKEN=$(java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" it.sara.demo.web.security.JwtTokenGenerator)
  ```

  PowerShell:

  ```powershell
  .\mvnw.cmd -q test-compile dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=target/cp.txt"
  $TOKEN = java -cp "target/test-classes;target/classes;$(Get-Content target/cp.txt)" it.sara.demo.web.security.JwtTokenGenerator
  ```

## 3. Call the API

Search users with `POST`. This needs the `users:read` scope, and every body field is optional:

| Field | Default | Description |
|---|---|---|
| `query` | none | Case-insensitive text searched in first name, last name and email |
| `offset` | 0 | Index of the first result |
| `limit` | 10 | Number of results, from 1 to 100 |
| `order` | `BY_LASTNAME` | `BY_FIRSTNAME`, `BY_FIRSTNAME_DESC`, `BY_LASTNAME` or `BY_LASTNAME_DESC` |

```bash
curl -X POST http://localhost:8080/user/v1/user \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"query":"ro","order":"BY_FIRSTNAME","limit":2}'
```

Add a user with `PUT`. This needs the `users:write` scope. The phone number must be a valid Italian number; it is
stored as `+39` followed by digits.

```bash
curl -X PUT http://localhost:8080/user/v1/user \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"firstName":"Giorgio","lastName":"Neri","email":"giorgio.neri@example.com","phoneNumber":"+39 333 765 4321"}'
```

PowerShell: use `Invoke-RestMethod`. In Windows PowerShell 5.1, `curl` is an alias of `Invoke-WebRequest`, and
`curl.exe` can break JSON bodies that contain spaces.

```powershell
$headers = @{ Authorization = "Bearer $TOKEN" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/user/v1/user -Headers $headers `
  -ContentType 'application/json' -Body '{"query":"ro","order":"BY_FIRSTNAME","limit":2}'
Invoke-RestMethod -Method Put -Uri http://localhost:8080/user/v1/user -Headers $headers `
  -ContentType 'application/json' `
  -Body '{"firstName":"Giorgio","lastName":"Neri","email":"giorgio.neri@example.com","phoneNumber":"+39 333 765 4321"}'
```

## 4. Read the responses

Every response, errors included, has HTTP status 200. The real outcome is in `status.code`:

| `status.code` | Meaning |
|---|---|
| 200 | Success |
| 400 | Invalid input (the message names the field) or malformed body |
| 401 | Missing, malformed, badly signed, expired or wrong-issuer token |
| 403 | Valid token without the required scope |
| 404 / 405 / 406 / 415 | Unknown path, unsupported method, `Accept` header without JSON, unsupported content type |
| 500 | Unexpected error; details are logged on the server, never returned |

`status.traceId` identifies the request: an error with a given trace id can be found in the server log.

## 5. Run the tests

```bash
./mvnw test
```

The tests use their own test-only configuration and do not need `JWT_SECRET`.
