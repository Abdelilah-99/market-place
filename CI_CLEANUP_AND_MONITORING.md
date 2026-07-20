# CI Cleanup Strategy and Monitoring Deployment Incidents

This document explains how Jenkins controls disk usage during build and deployment, what data the cleanup is allowed to remove, and the two monitoring deployment problems encountered in builds 55 and 56.

## Why cleanup is necessary

The deployment host has limited disk capacity. Java multi-stage images, frontend dependencies, Docker layers, Jenkins workspaces, container logs, and monitoring history can otherwise grow until Docker cannot export a new image or OpenSearch applies a read-only disk watermark.

The strategy therefore combines four controls:

1. prevent unnecessary builds;
2. release temporary build data as soon as possible;
3. bound persistent logs and monitoring history;
4. preserve domain data and stop safely if cleanup cannot provide enough space.

## Deployment lifecycle

`scripts/ci/deploy_local.sh` performs deployment in this order:

1. Create the shared Docker network and required external volumes when missing.
2. Synchronize certificates and JWT keys when Jenkins supplied a local certificate bundle. If no bundle exists, deployment expects the external certificate volumes to have already been prepared on the Docker host.
3. Run standard Docker cleanup.
4. Recreate older application containers without rebuilding them so obsolete runtime Maven and Gradle cache mounts are detached.
5. Remove the now-unused legacy cache volumes.
6. Read `.jenkins-state/last_successful_commit` and compare it with `HEAD`.
7. Start infrastructure without forced rebuilding.
8. Rebuild only application services affected since the last successful commit. Missing containers are always deployed.
9. Release transient build layers after each service build.
10. Run final standard cleanup and retry removal of legacy cache volumes.

Certificate lifecycle changes deliberately force all certificate consumers to restart. If the stored commit is absent or invalid, a full deployment is performed.

## Docker cleanup levels

The shared implementation is `scripts/ci/docker_cleanup.sh`.

### Standard cleanup

`docker_cleanup` is used before and after deployment. It:

- reports Docker disk usage before and after cleanup;
- prunes stopped containers;
- prunes unused networks;
- prunes unused images older than 24 hours by default;
- prunes builder cache older than 24 hours.

The age is configurable with `CI_DOCKER_PRUNE_UNTIL`. Setting `CI_DOCKER_PRUNE_ALL_IMAGES=false` restricts image cleanup to dangling images. All automatic Docker cleanup can be disabled with `CI_DOCKER_CLEANUP=false`.

### Build-time cleanup

`docker_build_cleanup` runs after each successful Compose build. It removes dangling images and all unused builder cache immediately. This sacrifices some rebuild speed, but prevents temporary layers from every Java multi-stage build accumulating until the end of a deployment.

Images used by running containers remain protected by Docker.

### Low-space cleanup

Before rebuilding an application, `docker_ensure_space` checks the filesystem containing Docker's data root. The default requirement is 8 GiB free and can be changed with `CI_DOCKER_MIN_FREE_GB`.

If available space is below the threshold, `docker_cleanup_aggressive` runs:

```bash
docker system prune -a -f
```

It removes all unused containers, networks, images, and build cache regardless of age. It intentionally does not pass `--volumes`.

Free space is checked again after cleanup. If it is still below the threshold, deployment stops instead of risking a partial build or deleting persistent data.

### Legacy build-cache volumes

Older Compose definitions mounted dependency caches into runtime containers even though Maven and Gradle are not used at runtime. Once the containers have been recreated without those mounts, these known cache-only volumes can be removed:

- `products-service_backend_m2`
- `gateway_backend_m2`
- `gateway_maven_cash`
- `users-service_gradle-cache`
- `media-service_media_m2`

Removal is best-effort. Docker refuses to remove a volume that is still attached, so a failed removal does not interrupt deployment.

## Data that cleanup preserves

The CI cleanup scripts never automatically prune Docker volumes. This protects:

- MongoDB domain databases;
- OpenSearch data;
- uploaded media;
- Prometheus and Grafana state;
- certificate and JWT volumes;
- Jenkins data;
- other named application volumes.

Aggressive cleanup can remove an unused image, but it cannot remove an image backing a running container. Required images can also be rebuilt or pulled during deployment.

## Jenkins workspace cleanup

After test reports and CI logs have been archived, `scripts/ci/workspace_cleanup.sh` removes reproducible workspace output:

- Maven `target` directories;
- the users-service Gradle `build` directory;
- frontend `node_modules`, `dist`, and `coverage`;
- `.jenkins-state/logs` after Jenkins archives the logs.

It retains reusable `.jenkins-state/m2` and `.jenkins-state/gradle` dependency caches and `.jenkins-state/last_successful_commit` for selective deployment.

Archived deployment logs remain available under the build record, for example:

```text
/var/jenkins_home/jobs/marketo/builds/<BUILD_NUMBER>/archive/.jenkins-state/logs/deploy-local.log
```

Jenkins also keeps only 10 build records and five artifact sets, preventing indefinite controller storage growth.

## Other retention controls

Cleanup is supported by bounded runtime retention:

- container JSON logs use three files of at most 10 MB each;
- Prometheus retains at most 15 days and 2 GB of metrics;
- host disk alerts warn below 20% or 8 GiB free and become critical below 10% or 4 GiB free;
- Kafka and application-domain retention policies limit old messages and expired records;
- a systemd cleanup timer can run safe cleanup daily and never prunes volumes.

## Monitoring incident 1: container-name conflict

### Symptom

Build 55 failed while starting the root monitoring stack:

```text
Conflict. The container name "/node-exporter" is already in use
```

### Root cause

Docker Compose derives its project name from the working directory unless one is explicitly supplied. The existing monitoring containers had been created from a checkout named `market-place`, while Jenkins deployed from a workspace named `marketo`.

Both Compose projects used fixed `container_name` values such as `node-exporter`, `prometheus`, and `grafana`. The `marketo` project therefore considered the existing containers foreign and attempted to create new containers with names already reserved by the `market-place` project.

The initial log was misleading because Jenkins caught the deployment error, successfully rolled back to the previous commit, and then rethrew the original error. The visible end of the console log consequently showed a successful gateway build and rollback followed by exit code 1. The archived `deploy-local.log` contained the actual conflict.

### Fix

The root stack now always runs with:

```bash
docker compose --project-name market-place ...
```

Only the root monitoring Compose stack needs this override. Service Compose projects already run inside stable service-specific directories. With the stable project name, Compose recognizes and updates the existing monitoring containers and continues using the existing `market-place_prometheus_data` and `market-place_grafana_data` volumes.

Commit: `60781a9 fix(ci): stabilize monitoring compose project`.

## Monitoring incident 2: Prometheus configuration bind mount

### Symptom

After the project-name fix, build 56 adopted and recreated the monitoring containers but Prometheus failed to start:

```text
error mounting "/var/jenkins_home/workspace/marketo/prometheus/prometheus.yml"
to "/etc/prometheus/prometheus.yml": not a directory
```

### Root cause

Jenkins runs in a container while its Docker CLI controls the host Docker daemon. This Docker-outside-of-Docker arrangement has two filesystem views:

- the Jenkins container can see `/var/jenkins_home/workspace/marketo/...`;
- the Docker daemon resolves bind-mount source paths on the Docker host, not inside the Jenkins container.

The Prometheus configuration file existed in the Jenkins checkout, but the daemon could not resolve that source as the same host file. Docker therefore encountered a file-versus-directory mismatch when creating the bind mount. Changing permissions would not solve the path-namespace mismatch.

### Fix

Prometheus now uses a small repository-owned image:

```dockerfile
FROM prom/prometheus

COPY prometheus.yml /etc/prometheus/prometheus.yml
COPY rules/ /etc/prometheus/rules/
```

Compose builds this image from the `prometheus` directory and no longer bind-mounts the configuration or rules from the Jenkins workspace. Build contexts are sent to Docker by the client, so they work even when Jenkins and the Docker daemon do not share identical absolute paths.

Persistent metrics and certificates remain mounted as Docker volumes:

- `prometheus_data:/prometheus`
- `prometheus-certs:/prometheus/certs:ro`

The image build and Compose configuration were validated, and `promtool` found both alert rules successfully. Full local configuration validation requires the production client certificate volume, which is supplied at runtime.

Commit: `50d3932 fix(ci): bake Prometheus configuration into image`.

## Troubleshooting future deployment failures

The console tail may show rollback activity instead of the original error. Always inspect the archived deployment log first:

```bash
find /var/jenkins_home/jobs/marketo/builds \
  -path '*/archive/.jenkins-state/logs/deploy-local.log' \
  -printf '%T@ %p\n' | sort -nr | head -1
```

Useful read-only disk diagnostics are:

```bash
source scripts/ci/docker_cleanup.sh
docker_disk_report
docker system df -v
```

Do not add `--volumes` to automatic prune commands. A large "reclaimable" volume figure is not proof that the volumes are disposable; it may include databases, media, monitoring history, or certificates belonging to temporarily stopped services.
