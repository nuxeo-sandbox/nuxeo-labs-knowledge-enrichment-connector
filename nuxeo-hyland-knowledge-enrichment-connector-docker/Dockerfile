# vim:set ft=dockerfile:
ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG VERSION
ARG SCM_REF
ARG BUILD_TAG
ARG CLID
ARG PACKAGES

LABEL com.customer.sample.version=${VERSION}
LABEL com.customer.sample.scm-ref=${SCM_REF}
LABEL com.customer.sample.build-tag=${BUILD_TAG}

# We need to be root to run yum commands
USER 0

# Set UTF8 default locale
ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    LC_ALL="en_US.UTF-8"

# Install repos
RUN dnf -y install epel-release && dnf -y install https://mirrors.rpmfusion.org/free/el/rpmfusion-free-release-9.noarch.rpm && \
    dnf -y install https://rpms.remirepo.net/enterprise/remi-release-9.rpm

RUN dnf config-manager --enable ol9_codeready_builder

#Install latest Imagemagick and codecs
RUN dnf -y install librsvg2-tools libwebp-tools

# Set back original (nuxeo) user
USER 900

COPY --chown=nuxeo:0 target/nuxeo-hyland-knowledge-enrichment-connector-package-*.zip /home/nuxeo/local-packages/nuxeo-hyland-knowledge-enrichment-connector-package.zip

RUN /install-packages.sh --offline /home/nuxeo/local-packages/nuxeo-hyland-knowledge-enrichment-connector-package.zip
