/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.atp.jdbc;

import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.jdbc.BasicJdbcConfiguration;

import java.security.SecureRandom;

/**
 * Configuration properties for the automated oracle wallet download and configuration.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
@Context
public class AutonomousDatabaseConfiguration {

    private static final int GENERATED_WALLET_SECRET_LENGTH = 32;

    private static final int MIN_WALLET_SECRET_LENGTH = 8;

    private static final char[] GENERATED_WALLET_SECRET_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final char[] GENERATED_WALLET_SECRET_NUMBERS = "0123456789".toCharArray();

    private static final char[] GENERATED_WALLET_SECRET_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String ocid;

    private String walletPassword;

    private GenerateAutonomousDatabaseWalletDetails.GenerateType walletType;

    private String serviceAlias;

    /**
     * Suffix for the default service alias. If {@link #serviceAlias} is not configured,
     * the default service alias will be computed as (dbName + "_" + serviceAliasSuffix).
     * Defaults to "high".
     */
    private String serviceAliasSuffix = "high";

    /**
     * @return autonomous database ocid
     */
    public String getOcid() {
        return ocid;
    }

    /**
     * @param ocid autonomous database ocid
     */
    public void setOcid(String ocid) {
        this.ocid = ocid;
    }

    /**
     * @return wallet password
     */
    public synchronized String getWalletPassword() {
        if (walletPassword == null || walletPassword.isBlank()) {
            walletPassword = generateWalletPassword();
        }
        return walletPassword;
    }

    /**
     * @param walletPassword wallet password
     */
    public synchronized void setWalletPassword(String walletPassword) {
        if (walletPassword != null && !walletPassword.isBlank()) {
            validateConfiguredWalletSecret(walletPassword);
        }
        this.walletPassword = walletPassword;
    }

    /**
     * @return wallet type
     */
    public GenerateAutonomousDatabaseWalletDetails.GenerateType getWalletType() {
        return walletType;
    }

    /**
     * @param walletType wallet type
     */
    public void setWalletType(GenerateAutonomousDatabaseWalletDetails.GenerateType walletType) {
        this.walletType = walletType;
    }

    /**
     * @return service alias
     */
    public String getServiceAlias() {
        return serviceAlias;
    }

    /**
     * @param serviceAlias service alias
     */
    public void setServiceAlias(String serviceAlias) {
        this.serviceAlias = serviceAlias;
    }

    /**
     * @return service alias suffix. Defaults to "high" if not set.
     */
    public String getServiceAliasSuffix() {
        return serviceAliasSuffix;
    }

    /**
     * @param serviceAliasSuffix service alias suffix to use when service alias is not configured
     */
    public void setServiceAliasSuffix(String serviceAliasSuffix) {
        this.serviceAliasSuffix = serviceAliasSuffix;
    }

    private static String generateWalletPassword() {
        char[] password = new char[GENERATED_WALLET_SECRET_LENGTH];
        password[0] = randomChar(GENERATED_WALLET_SECRET_LETTERS);
        password[1] = randomChar(GENERATED_WALLET_SECRET_NUMBERS);
        for (int i = 2; i < password.length; i++) {
            password[i] = randomChar(GENERATED_WALLET_SECRET_CHARS);
        }
        shuffle(password);
        return new String(password);
    }

    private static void validateConfiguredWalletSecret(String walletPassword) {
        if (walletPassword.length() < MIN_WALLET_SECRET_LENGTH
                || walletPassword.chars().noneMatch(Character::isLetter)
                || walletPassword.chars().noneMatch(AutonomousDatabaseConfiguration::isNumberOrSpecialCharacter)) {
            throw new IllegalArgumentException(
                    "wallet-password must be at least eight characters long and include at least one letter and one numeric or special character");
        }
    }

    private static boolean isNumberOrSpecialCharacter(int character) {
        return Character.isDigit(character) || (!Character.isLetterOrDigit(character) && !Character.isWhitespace(character));
    }

    private static char randomChar(char[] chars) {
        return chars[SECURE_RANDOM.nextInt(chars.length)];
    }

    private static void shuffle(char[] password) {
        for (int i = password.length - 1; i > 0; i--) {
            int index = SECURE_RANDOM.nextInt(i + 1);
            char current = password[index];
            password[index] = password[i];
            password[i] = current;
        }
    }
}
