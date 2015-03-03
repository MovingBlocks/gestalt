/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.assets.test;

import com.google.common.base.Optional;
import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.invocation.InvocationOnMock;

/**
 * @author Immortius
 */
public class OptionalAnswer extends ReturnsEmptyValues {

    @Override
    public Object answer(InvocationOnMock invocation) {
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (returnType == Optional.class) {
            return Optional.absent();
        }
        return super.answer(invocation);
    }
}
