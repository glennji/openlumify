#!/bin/sh

keytool \
  -genkey \
  -keyalg RSA \
  -ext san=dns:localhost,ip:127.0.0.1,ip:::1 \
  -alias openlumify-vm.openlumify.org \
  -keystore openlumify-vm.openlumify.org.jks \
  -storepass password \
  -validity 360 \
  -keysize 2048

# For livereload in webapp/test/localhost.[cert|key]

keytool -export \
  -alias openlumify-vm.openlumify.org \
  -file localhost.der \
  -storepass password \
  -keystore openlumify-vm.openlumify.org.jks
openssl x509 -inform der -in localhost.der -out localhost.cert
mv localhost.cert ../../../web/war/src/main/webapp/test/
rm localhost.der

keytool -importkeystore \
  -srckeystore openlumify-vm.openlumify.org.jks \
  -storepass password \
  -destkeystore localhost.p12 \
  -deststoretype PKCS12
openssl pkcs12 -in localhost.p12  -nodes -nocerts -out localhost.key
mv localhost.key ../../../web/war/src/main/webapp/test/
rm localhost.p12
