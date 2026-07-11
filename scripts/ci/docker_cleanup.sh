#!/usr/bin/env bash

# Safe Docker cleanup shared by deployment scripts. This intentionally never
# prunes volumes because they contain databases, media, monitoring, Jenkins,
# and certificate data.
docker_cleanup() {
  if [[ "${CI_DOCKER_CLEANUP:-true}" != "true" ]]; then
    echo "[CD] Docker cleanup disabled."
    return
  fi

  local prune_until="${CI_DOCKER_PRUNE_UNTIL:-24h}"

  echo "[CD] Docker disk usage before cleanup:"
  docker system df || true
  echo "[CD] Pruning unused containers, networks, images, and builder cache older than ${prune_until}."
  docker container prune -f >/dev/null || true
  docker network prune -f >/dev/null || true

  if [[ "${CI_DOCKER_PRUNE_ALL_IMAGES:-true}" == "true" ]]; then
    docker image prune -a -f --filter "until=${prune_until}" >/dev/null || true
  else
    docker image prune -f >/dev/null || true
  fi

  docker builder prune -f --filter "until=${prune_until}" >/dev/null || true
  echo "[CD] Docker disk usage after cleanup:"
  docker system df || true
}
