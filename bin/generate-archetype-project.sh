#!/usr/bin/env bash
set -eu

DIR=$(cd $(dirname "$0") && pwd)
VISALLO_DIR=${DIR}/..
ARCHETYPE_JAR_DIR=$VISALLO_DIR/archetype/target

cd ${VISALLO_DIR}

mvn clean package -am -pl archetype

VERSION=$(find "${ARCHETYPE_JAR_DIR}" -name "openlumify-plugin-archetype-*.jar" | sed -e 's/.*openlumify-plugin-archetype-//' -e 's/\.jar$//')
echo "Using ${VERSION}"

(
cd ${DIR}

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
    -Dfile=${ARCHETYPE_JAR_DIR}/openlumify-plugin-archetype-${VERSION}.jar \
    -DgroupId=org.openlumify \
    -DartifactId=openlumify-plugin-archetype \
    -Dversion=${VERSION} \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:crawl
)

mvn archetype:generate -DarchetypeGroupId=org.openlumify -DarchetypeArtifactId=openlumify-plugin-archetype -DarchetypeVersion=${VERSION} $@
