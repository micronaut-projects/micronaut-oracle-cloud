/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oci.function.http;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.bind.DefaultRequestBinderRegistry;
import io.micronaut.http.bind.binders.RequestArgumentBinder;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.servlet.http.ServletBinderRegistry;

import javax.inject.Singleton;
import java.util.List;

@Internal
@Singleton
@Replaces(DefaultRequestBinderRegistry.class)
class FnBinderRegistry extends ServletBinderRegistry {
    /**
     * Default constructor.
     *
     * @param mediaTypeCodecRegistry The media type codec registry
     * @param conversionService      The conversion service
     * @param binders                Any registered binders
     */
    public FnBinderRegistry(MediaTypeCodecRegistry mediaTypeCodecRegistry, ConversionService conversionService, List<RequestArgumentBinder> binders) {
        super(mediaTypeCodecRegistry, conversionService, binders);
        this.byAnnotation.put(Body.class, new FnBodyBinder<>(conversionService, mediaTypeCodecRegistry));
    }
}
