package org.dizitart.no2.objects;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.WriteResult;
import org.dizitart.no2.exceptions.InvalidIdException;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.exceptions.ObjectMappingException;
import org.dizitart.no2.objects.data.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.dizitart.no2.DbTestOperations.getRandomTempDbFile;
import static org.junit.Assert.assertEquals;

/**
 * @author Anindya Chatterjee.
 */
public class ObjectRepositoryNegativeTest {
    private String dbPath = getRandomTempDbFile();
    private Nitrite db;

    @Before
    public void setUp() {
        db = Nitrite.builder()
                .filePath(dbPath)
                .openOrCreate();
    }

    @After
    public void close() throws IOException {
        db.close();
        db = null;
        Files.delete(Paths.get(dbPath));
    }

    @Test(expected = ObjectMappingException.class)
    public void testWithCircularReference() {
        ObjectRepository<WithCircularReference> repository = db.getRepository(WithCircularReference.class);

        WithCircularReference parent = new WithCircularReference();
        parent.setName("parent");
        WithCircularReference object = new WithCircularReference();
        object.setName("test");
        object.setParent(parent);
        // circular reference
        parent.setParent(object);

        WriteResult result = repository.insert(object);
        for (NitriteId id : result) {
            WithCircularReference instance = repository.getById(id);
            assertEquals(instance.getName(), object.getName());
            assertEquals(instance.getParent().getName(), object.getParent().getName());
        }
    }

    @Test(expected = ObjectMappingException.class)
    public void testWithCustomConstructor() {
        ObjectRepository<WithCustomConstructor> repository = db.getRepository(WithCustomConstructor.class);

        WithCustomConstructor object = new WithCustomConstructor("test", 2L);

        WriteResult result = repository.insert(object);
        for (NitriteId id : result) {
            WithCustomConstructor instance = repository.getById(id);
            assertEquals(object.getName(), instance.getName());
            assertEquals(object.getNumber(), instance.getNumber());
        }
    }

    @Test(expected = InvalidIdException.class)
    public void testWithEmptyStringId() {
        ObjectRepository<WithEmptyStringId> repository = db.getRepository(WithEmptyStringId.class);
        WithEmptyStringId object = new WithEmptyStringId();
        object.setName(""); // empty id value

        WriteResult result = repository.insert(object);
        for (NitriteId id : result) {
            WithEmptyStringId instance = repository.getById(id);
            assertEquals(instance, object);
        }
    }

    @Test(expected = InvalidIdException.class)
    public void testWithNullId() {
        ObjectRepository<WithNullId> repository = db.getRepository(WithNullId.class);
        WithNullId object = new WithNullId();

        WriteResult result = repository.insert(object);
        for (NitriteId id : result) {
            WithNullId instance = repository.getById(id);
            assertEquals(instance, object);
        }
    }

    @Test(expected = InvalidOperationException.class)
    public void testFindResultRemove() {
        ObjectRepository<String> repository = db.getRepository(String.class);
        repository.insert("test");
        RecordIterable<String> result = repository.find();
        result.iterator().remove();
    }

    @Test(expected = InvalidOperationException.class)
    public void testWithObjectId() {
        ObjectRepository<WithObjectId> repository = db.getRepository(WithObjectId.class);
        WithOutId id = new WithOutId();
        id.setName("test");
        id.setNumber(1);

        WithObjectId object = new WithObjectId();
        object.setWithOutId(id);
        repository.insert(object);
    }
}
