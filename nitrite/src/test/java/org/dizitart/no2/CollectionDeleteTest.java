package org.dizitart.no2;

import org.junit.Test;

import static org.dizitart.no2.filters.Filters.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionDeleteTest extends BaseCollectionTest {

    @Test
    public void testDelete() {
        insert();

        WriteResult writeResult = collection.remove(not(eq("lastName", null)));
        assertEquals(writeResult.getAffectedCount(), 3);

        Cursor cursor = collection.find();
        assertEquals(cursor.size(), 0);
    }

    @Test
    public void testDeleteWithOptions() {
        insert();

        RemoveOptions removeOptions = new RemoveOptions();
        removeOptions.setJustOne(true);

        WriteResult writeResult = collection.remove(not(eq("lastName", null)), removeOptions);
        assertEquals(writeResult.getAffectedCount(), 1);

        Cursor cursor = collection.find();
        assertEquals(cursor.size(), 2);
    }

    @Test
    public void testDeleteWithNonMatchingFilter() {
        insert();

        Cursor cursor = collection.find();
        assertEquals(cursor.size(), 3);

        WriteResult writeResult = collection.remove(eq("lastName", "a"));
        assertEquals(writeResult.getAffectedCount(), 0);
    }

    @Test
    public void testDeleteInEmptyCollection() {
        Cursor cursor = collection.find();
        assertEquals(cursor.size(), 0);

        WriteResult writeResult = collection.remove(not(eq("lastName", null)));
        assertEquals(writeResult.getAffectedCount(), 0);
    }

    @Test
    public void testClear() {
        collection.createIndex("firstName", IndexOptions.indexOptions(IndexType.Unique));
        insert();

        Cursor cursor = collection.find();
        assertEquals(cursor.size(), 3);
        assertTrue(collection.hasIndex("firstName"));

        boolean uniqueError = false;
        try {
            collection.insert(doc1);
        } catch (Exception e) {
            uniqueError = true;
        } finally {
            assertTrue(uniqueError);
        }

        collection.remove(ALL);

        cursor = collection.find();
        assertEquals(cursor.size(), 0);
        assertTrue(collection.hasIndex("firstName"));

        collection.insert(doc1);
        cursor = collection.find();
        assertEquals(cursor.size(), 1);
        assertTrue(collection.hasIndex("firstName"));
    }

    @Test
    public void testRemoveAll() {
        insert();
        WriteResult writeResult = collection.remove((Filter) null);
        assertEquals(writeResult.getAffectedCount(), 3);
    }

    @Test
    public void testRemoveDocument() {
        insert();

        WriteResult writeResult = collection.remove(doc1);
        assertEquals(writeResult.getAffectedCount(), 1);
        assertEquals(collection.size(), 2);

        writeResult = collection.remove(doc2);
        assertEquals(writeResult.getAffectedCount(), 1);
        assertEquals(collection.size(), 1);

        assertEquals(collection.find(eq("firstName", "fn1")).size(), 0);
        assertEquals(collection.find(eq("firstName", "fn2")).size(), 0);
        assertEquals(collection.find(eq("firstName", "fn3")).size(), 1);
    }
}
