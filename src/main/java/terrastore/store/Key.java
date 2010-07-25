/**
 * Copyright 2009 - 2010 Sergio Bossa (sergio.bossa@gmail.com)
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
package terrastore.store;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.terracotta.annotations.InstrumentedClass;

/**
 * @author Sergio Bossa
 */
@InstrumentedClass
public class Key implements Comparable<Key>, Serializable {

    private static final long serialVersionUID = 12345678901L;
    private static final Charset CHARSET = Charset.forName("UTF-8");
    //
    private final byte[] bytes;

    public Key(String key) {
        this.bytes = key.getBytes(CHARSET);
    }

    public Key(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] toBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return new String(bytes, CHARSET);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Key) {
            Key other = (Key) obj;
            return Arrays.equals(this.bytes, other.bytes);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public int compareTo(Key other) {
        return this.toString().compareTo(other.toString());
    }
}