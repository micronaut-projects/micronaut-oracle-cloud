/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.SessionKeySupplier;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Secondary;
import jakarta.inject.Singleton;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link SessionKeySupplier} implementation that lazily generates RSA keys to avoid generating keys
 * that go unused. This is a singleton that is used for all session keys in the application context.
 * <p>
 * Ideally this should also be shared with the bootstrap context in the future.
 *
 * @since 4.2.0
 * @author Jonas Konrad
 */
@Singleton
@Secondary
@BootstrapContextCompatible
final class LazySessionKeySupplier implements SessionKeySupplier {
    private static final KeyPairGenerator GENERATOR;

    private final Lock lock = new ReentrantLock();
    private volatile KeyPair keyPair = null;

    static {
        try {
            GENERATOR = KeyPairGenerator.getInstance("RSA");
            GENERATOR.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e.getMessage(), e);
        }
    }

    @Override
    public KeyPair getKeyPair() {
        KeyPair kp = keyPair;
        if (kp == null) {
            lock.lock();
            try {
                kp = keyPair;
                if (kp == null) {
                    kp = GENERATOR.generateKeyPair();
                    keyPair = kp;
                }
            } finally {
                lock.unlock();
            }
        }
        return kp;
    }

    @Override
    public void refreshKeys() {
        keyPair = null;
    }
}
