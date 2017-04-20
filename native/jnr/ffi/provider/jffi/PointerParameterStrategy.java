/*
 * Copyright (C) 2012 Wayne Meissner
 *
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jnr.ffi.provider.jffi;

import com.kenai.jffi.ObjectParameterType;
import jnr.ffi.Pointer;

/**
 *
 */
public final class PointerParameterStrategy extends ParameterStrategy {
    public static final PointerParameterStrategy DIRECT = new PointerParameterStrategy(StrategyType.DIRECT);
    public static final PointerParameterStrategy HEAP = new PointerParameterStrategy(StrategyType.HEAP);

    PointerParameterStrategy(StrategyType type) {
        super(type, ObjectParameterType.create(ObjectParameterType.ARRAY, ObjectParameterType.BYTE));
    }

    @Override
    public long address(Object o) {
        return address((Pointer) o);
    }

    public long address(Pointer pointer) {
        return pointer != null ? pointer.address() : 0L;
    }

    @Override
    public Object object(Object o) {
        return ((Pointer) o).array();
    }

    @Override
    public int offset(Object o) {
        return ((Pointer) o).arrayOffset();
    }

    @Override
    public int length(Object o) {
        return ((Pointer) o).arrayLength();
    }
}
