#!/usr/bin/env bash

mkdir $HOME/.oci
echo "[DEFAULT]" >> $HOME/.oci/config
echo "user=${OCI_USER}" >> $HOME/.oci/config
echo "fingerprint=${OCI_FINGERPRINT}" >> $HOME/.oci/config
echo "pass_phrase=${OCI_PASSPHRASE}" >> $HOME/.oci/config
echo "region=${OCI_REGION}" >> $HOME/.oci/config
echo "tenancy=${OCI_TENANCY}" >> $HOME/.oci/config
echo "key_file=$HOME/.oci/key.pem" >> $HOME/.oci/config
echo "${OCI_KEY_FILE}" >> $HOME/.oci/key.pem