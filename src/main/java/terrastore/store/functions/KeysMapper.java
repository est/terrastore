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
package terrastore.store.functions;

import java.util.HashMap;
import java.util.Map;
import terrastore.store.operators.Function;
import static terrastore.util.concurrent.ConcurrentUtils.*;

/**
 * @author Sergio Bossa
 */
public class KeysMapper implements Function {

    @Override
    public Map<String, Object> apply(String key, Map<String, Object> value, Map<String, Object> parameters) {
        exitOnTimeout();
        Map<String, Object> keys = new HashMap<String, Object>(1);
        keys.put("keys", key);
        return keys;
    }
}
