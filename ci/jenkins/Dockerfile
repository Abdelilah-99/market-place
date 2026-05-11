FROM jenkins/jenkins:lts-jdk21

USER root
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl git gnupg vim \
  && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
  && apt-get install -y --no-install-recommends nodejs \
  && curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-25.0.0.tgz | tar -xzf - -C /usr/local/bin --strip-components=1 docker/docker \
  && chmod +x /usr/local/bin/docker \
    && curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
    && chmod +x /usr/local/bin/docker-compose \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd -f docker \
  && (groupmod -g 981 docker || true) \
  && usermod -aG docker jenkins

USER jenkins
