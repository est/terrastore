/**
 * Copyright 2009 - 2011 Sergio Bossa (sergio.bossa@gmail.com)
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
package terrastore.store.impl;

import terrastore.server.Keys;
import java.io.File;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import terrastore.startup.Constants;
import terrastore.store.BackupManager;
import terrastore.store.Bucket;
import terrastore.store.Key;
import terrastore.store.Value;
import terrastore.util.collect.Sets;
import static org.easymock.EasyMock.*;

/**
 * @author Sergio Bossa
 */
public class DefaultBackupManagerTest {

    private static final Key KEY_1 = new Key("KEY1");
    private static final Key KEY_2 = new Key("KEY2");
    private static final Value JSON_VALUE_1 = new Value("{\"test1\":\"test1\"}".getBytes(Charset.forName("UTF-8")));
    private static final Value JSON_VALUE_2 = new Value("{\"test2\":\"test2\"}".getBytes(Charset.forName("UTF-8")));

    @Before
    public void setUp() {
        System.setProperty(Constants.TERRASTORE_HOME, System.getProperty("java.io.tmpdir"));
        new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + Constants.BACKUPS_DIR).mkdir();
    }

    @Test
    public void testExportImportWithJsonValue() throws Exception {
        Bucket bucket = createMock(Bucket.class);

        bucket.keys();
        expectLastCall().andReturn(new Keys(Sets.hash(KEY_1, KEY_2)));
        bucket.getName();
        expectLastCall().andReturn("bucket").anyTimes();
        bucket.get(KEY_1);
        expectLastCall().andReturn(JSON_VALUE_1).once();
        bucket.get(KEY_2);
        expectLastCall().andReturn(JSON_VALUE_2).once();
        bucket.put(KEY_1, JSON_VALUE_1);
        expectLastCall().once();
        bucket.put(KEY_2, JSON_VALUE_2);
        expectLastCall().once();

        replay(bucket);

        BackupManager backupManager = new DefaultBackupManager();

        backupManager.exportBackup(bucket, "test");

        backupManager.importBackup(bucket, "test");

        verify(bucket);
    }

    @Test
    public void testBackupOverwriting() throws Exception {
        Bucket bucket = createMock(Bucket.class);

        bucket.keys();
        expectLastCall().andReturn(new Keys(Sets.hash(KEY_1)));
        bucket.getName();
        expectLastCall().andReturn("bucket").anyTimes();
        bucket.get(KEY_1);
        expectLastCall().andReturn(JSON_VALUE_1).once();
        bucket.put(KEY_1, JSON_VALUE_1);
        expectLastCall().once();

        replay(bucket);

        BackupManager backupManager = new DefaultBackupManager();

        backupManager.exportBackup(bucket, "test");

        backupManager.importBackup(bucket, "test");

        verify(bucket);
    }
}