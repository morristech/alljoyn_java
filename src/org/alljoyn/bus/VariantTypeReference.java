/*
 * Copyright 2009-2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.alljoyn.bus;

/**
 * Used when calling {@link Variant#getObject(Class)} where the wrapped
 * object is a generic type such as {@code Map<K, V>}.  For example, the
 * proper syntax is {@code getObject(new VariantTypeReference<TreeMap<String,
 * String>>() {})} when the wrapped object is a {@code TreeMap<String,
 * String>}.
 */
public abstract class VariantTypeReference<T> {
}
