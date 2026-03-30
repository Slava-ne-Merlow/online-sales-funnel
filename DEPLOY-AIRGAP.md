# Air-Gapped Deploy

This project is prepared for deployment to a VM without internet access.

## What CI builds

The GitHub Actions workflow produces one artifact:

- `airgap-release_<version>.tar.gz`

The archive contains:

- `images_<version>.tar`
- `docker-compose.yml`
- `.env.example`
- `DEPLOY-AIRGAP.md`

## How to deploy on the VM

1. Copy `airgap-release_<version>.tar.gz` to the VM.
2. Unpack it:

```bash
mkdir -p /opt/online-sales-funnel
tar -xzf airgap-release_<version>.tar.gz -C /opt/online-sales-funnel
cd /opt/online-sales-funnel
```

3. Prepare environment:

```bash
cp .env.example .env
```

Set at least:

- `APP_VERSION`
- `FRONTEND_PORT`
- `BACKEND_PORT`
- `POSTGRES_DB`
- `POSTGRES_USERNAME`
- `POSTGRES_PASSWORD`
- `POSTGRES_JDBC_URL`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET`

4. Load docker images from the bundle:

```bash
docker load -i images_<version>.tar
```

5. Start the stack:

```bash
docker compose up -d
```

After startup:

- frontend will be available on `http://<vm-host>:<FRONTEND_PORT>`
- backend API will be available on `http://<vm-host>:<BACKEND_PORT>`
- frontend will still proxy `/api` to backend internally

## Why this works without internet

- `docker-compose.yml` uses `image:` instead of `build:`
- every service has `pull_policy: never`
- all required images are already inside `images_<version>.tar`
- frontend talks to backend through Nginx proxy on `/api`, so no rebuild is needed for API URL
- frontend and backend ports are configurable through `.env`, so they can be moved away from ports already used on the VM
