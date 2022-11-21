#!/usr/bin/env bash

echo "Configuration $HOME/.oci"

mkdir $HOME/.oci
echo "[DEFAULT]" >> $HOME/.oci/config
echo "user=${OCI_USER_ID}" >> $HOME/.oci/config
echo "fingerprint=${OCI_FINGERPRINT}" >> $HOME/.oci/config
echo "pass_phrase=${OCI_PASSPHRASE}" >> $HOME/.oci/config
echo "region=${OCI_REGION}" >> $HOME/.oci/config
echo "tenancy=${OCI_TENANT_ID}" >> $HOME/.oci/config
echo "key_file=$HOME/.oci/key.pem" >> $HOME/.oci/config
echo "${OCI_PRIVATE_KEY}" >> $HOME/.oci/key.pem
chmod 400 $HOME/.oci/key.pem