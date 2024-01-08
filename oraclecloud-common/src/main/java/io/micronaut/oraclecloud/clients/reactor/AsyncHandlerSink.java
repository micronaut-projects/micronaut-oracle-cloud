/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.oraclecloud.clients.reactor;

import com.oracle.bmc.responses.AsyncHandler;
import reactor.core.publisher.MonoSink;

/**
 * Bridges the {@link AsyncHandler} interface to an RxJava {@link reactor.core.publisher.MonoSink}.
 *
 * @param <Req> The request type
 * @param <Res> The response type
 * @author graemerocher
 * @since 2.0.0
 */
public class AsyncHandlerSink<Req, Res> implements AsyncHandler<Req, Res> {
    private final MonoSink<Res> emitter;

    public AsyncHandlerSink(MonoSink<Res> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onSuccess(Req req, Res res) {
        emitter.success(res);
    }

    @Override
    public void onError(Req req, Throwable error) {
        emitter.error(error);
    }
}
