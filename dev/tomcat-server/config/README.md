
Linux
-----

```bash
keytool -importkeystore -srckeystore openlumify-vm.openlumify.org.jks -destkeystore openlumify-vm.openlumify.org.p12 -srcalias openlumify-vm.openlumify.org -srcstoretype jks -deststoretype pkcs12
# password is password
openssl pkcs12 -in openlumify-vm.openlumify.org.p12 -out openlumify-vm.openlumify.org.pem
certutil -d sql:$HOME/.pki/nssdb -A -t "P,," -n openlumify-vm.openlumify.org -i openlumify-vm.openlumify.org.pem
```

completely restart Chrome

