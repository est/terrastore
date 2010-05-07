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

import terrastore.communication.Node;
import terrastore.communication.ProcessingException;
import terrastore.router.MissingRouteException;
import terrastore.router.Router;
import terrastore.store.Bucket;
import terrastore.store.Store;
import terrastore.store.StoreOperationException;

/**
 * @author Sergio Bossa
 */
public class ImportBackupCommand extends AbstractCommand {

    private final String bucketName;
    private final String source;

    public ImportBackupCommand(String bucketName, String source) {
        this.bucketName = bucketName;
        this.source = source;
    }

    @Override
    public Object executeOn(Router router) throws MissingRouteException, ProcessingException {
        Node node = router.routeToLocalNode();
        return node.send(this);
    }

    public Object executeOn(Store store) throws StoreOperationException {
        Bucket bucket = store.getOrCreate(bucketName);
        bucket.importBackup(source);
        return null;
    }
}
