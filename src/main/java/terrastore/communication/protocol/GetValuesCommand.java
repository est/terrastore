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
package terrastore.communication.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import terrastore.communication.CommunicationException;
import terrastore.communication.Node;
import terrastore.communication.ProcessingException;
import terrastore.router.MissingRouteException;
import terrastore.router.Router;
import terrastore.store.Bucket;
import terrastore.store.Key;
import terrastore.store.Store;
import terrastore.store.StoreOperationException;
import terrastore.store.Value;
import terrastore.store.features.Predicate;
import terrastore.store.operators.Condition;
import terrastore.util.collect.Maps;

/**
 * @author Sergio Bossa
 */
public class GetValuesCommand extends AbstractCommand<Map<Key, Value>> {

    private final String bucketName;
    private final Set<Key> keys;
    private final boolean conditional;
    private final Predicate predicate;
    private final Condition valueCondition;

    public GetValuesCommand(GetValuesCommand command, Set<Key> keys) {
        this.bucketName = command.bucketName;
        this.conditional = command.conditional;
        this.predicate = command.predicate;
        this.valueCondition = command.valueCondition;
        this.keys = keys;
    }

    public GetValuesCommand(String bucketName, Set<Key> keys) {
        this.bucketName = bucketName;
        this.keys = keys;
        this.conditional = false;
        this.predicate = null;
        this.valueCondition = null;
    }

    public GetValuesCommand(String bucketName, Set<Key> keys, Predicate predicate, Condition valueCondition) {
        this.bucketName = bucketName;
        this.keys = keys;
        this.conditional = true;
        this.predicate = predicate;
        this.valueCondition = valueCondition;
    }

    @Override
    public Map<Key, Value> executeOn(Router router) throws CommunicationException, MissingRouteException, ProcessingException {
        Map<Node, Set<Key>> nodeToKeys = router.routeToNodesFor(bucketName, keys);
        Map<Key, Value> result = new HashMap<Key, Value>();
        for (Map.Entry<Node, Set<Key>> nodeToKeysEntry : nodeToKeys.entrySet()) {
            Node node = nodeToKeysEntry.getKey();
            Set<Key> nodeKeys = nodeToKeysEntry.getValue();
            GetValuesCommand command = new GetValuesCommand(this, nodeKeys);
            result.putAll(node.<Map<Key, Value>>send(command));
        }
        return Maps.serializing(result);
    }

    public Map<Key, Value> executeOn(Store store) throws StoreOperationException {
        Bucket bucket = store.get(bucketName);
        if (bucket != null) {
            Map<Key, Value> entries = new HashMap<Key, Value>();
            for (Key key : keys) {
                Value value = null;
                if (!conditional) {
                    value = bucket.get(key);
                } else {
                    value = bucket.conditionalGet(key, predicate, valueCondition);
                }
                if (value != null) {
                    entries.put(key, value);
                }
            }
            return Maps.serializing(entries);
        } else {
            return new HashMap<Key, Value>(0);
        }
    }
}
