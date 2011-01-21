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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A DBus/AllJoyn datatype whose type information is encoded into the
 * value field.
 */
public class Variant {

    /** Wrapped object */
    private Object value;

    /** Wrapped object signature */
    private String signature;

    /** Wrapped native object */
    private long handle;

    /**
     * Creates a variant that can represent any AllJoyn data type.
     * This constructor is typically used by message receivers (method and
     * signal handlers) when the actual data type represented by the variant is
     * not known.
     */
    Variant() {}

    /**
     * Creates a Variant that represents a specific AllJoyn data type and value.
     * This constructor is typically used by message senders (method calls and
     * signal emitters) when a specific Variant type and value are known at the
     * time the Variant is created.
     *
     * @param value object wrapped inside this Variant
     */
    public <T> Variant(T value) {
        this.value = value;
    }

    /**
     * Creates a Variant with an explicit signature that represents a specific
     * AllJoyn data type and instance value.
     * This constructor is typically used by message senders (method calls and
     * signal emitters) when a specific Variant type and value are known at the
     * time the Variant is created.
     *
     * @param value object wrapped inside the Variant
     * @param signature the DBus signature of value
     */
    public <T> Variant(T value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    /** Releases native resources */
    private synchronized native void destroy();

    /** Releases resources. */
    protected void finalize() {
        destroy();
    }

    /** Gets the wrapped native object. */
    long getMsgArg() { return handle; }

    /** Sets the wrapped native object to the MsgArg. */
    native void setMsgArg(long msgArg);

    /** Gets the wrapped object. */
    Object getValue() { return value; }

    /** Gets the DBus type signature of the wrapped object. */
    String getSignature() throws AnnotationBusException {
        if (value != null) {
            return Signature.typeSig(value.getClass(), signature);
        } else {
            return null;
        }
    }

    /**
     * Gets the object wrapped by this Variant when object is specified by Type.
     *
     * @param type the type of the return value.
     * @return the value of the Variant.
     * @throws BusException if Variant data cannot be unmarshalled
     * @throws ClassCastException if Variant data cannot be converted to requested type
     */
    public <T> T getObject(Type type) throws BusException {
        if (value == null) {
            value = MsgArg.unmarshal(handle, type);
        }
        @SuppressWarnings(value = "unchecked")
        T o = (T) value;
        return o;
    }

    /**
     * Gets the object wrapped by this Variant when object is specified by Class.
     *
     * @param type the type of the return value.
     * @return the value of the Variant.
     * @throws BusException if Variant data cannot be unmarshalled
     * @throws ClassCastException if Variant data cannot be converted to requested type
     */
    public <T> T getObject(Class<T> type) throws BusException {
        if (value == null) {
            value = MsgArg.unmarshal(handle, type);
        }
        @SuppressWarnings(value = "unchecked")
        T o = (T) value;
        return o;
    }

    /**
     * Gets the object wrapped by this Variant when the wrapped object is a
     * generic type.
     *
     * @param type the VariantTypeReference of the return value
     * @return the value of the Variant
     * @throws BusException if Variant data cannot be unmarshalled
     * @throws ClassCastException if Variant data cannot be converted to requested type
     * @see VariantTypeReference
     */
    public <T> T getObject(VariantTypeReference<T> type) throws BusException {
        if (value == null) {
            Type sc = type.getClass().getGenericSuperclass();
            value = MsgArg.unmarshal(handle, ((ParameterizedType) sc).getActualTypeArguments()[0]);
        }
        @SuppressWarnings(value = "unchecked")
        T o = (T) value;
        return o;
    }

    /**
     * Indicates whether some other Variant is "equal to" this one.
     *
     * @param obj the other Variant
     * @return {@code true} when variants values are equal to one another
     */
    public boolean equals(Object obj) {
        try {
            Object obj1 = getObject(value.getClass());
            Object obj2 = ((Variant) obj).getObject(value.getClass());
            return obj1.equals(obj2);
        } catch (BusException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
    }
}
