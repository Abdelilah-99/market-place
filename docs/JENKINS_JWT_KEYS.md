# Jenkins JWT Key Rotation

The previous JWT private key was committed to Git, so it must be treated as compromised. Rotate it and store the new key pair only in Jenkins credentials.

## Generate a New Pair

```bash
bash scripts/generate-jwt-keys.sh /tmp/marketo-jwt-keys
```

This creates:

- `/tmp/marketo-jwt-keys/private.pem`
- `/tmp/marketo-jwt-keys/public.pem`

## Jenkins Credentials

Create two Jenkins **File** credentials:

| Credential ID | File |
| --- | --- |
| `jwt-private-key` | `private.pem` |
| `jwt-public-key` | `public.pem` |

The `Jenkinsfile` exposes those files to `scripts/ci/setup_env.sh`.

## Runtime Paths

The CI setup copies keys into the Docker cert folders:

- `users-service/certs/jwt-private.pem`
- `gateway/certs/jwt-public.pem`

The deploy script syncs those folders into Docker volumes mounted at:

- `users-service`: `/app/resources/certs/jwt-private.pem`
- `gateway`: `/app/resources/certs/jwt-public.pem`

The services read them with:

```properties
jwt.private-key=file:/app/resources/certs/jwt-private.pem
jwt.public-key=file:/app/resources/certs/jwt-public.pem
```

## After Rotation

All old JWTs signed by the leaked private key become invalid after both services restart with the new keys.
