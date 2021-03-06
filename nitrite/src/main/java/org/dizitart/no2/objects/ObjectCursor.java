package org.dizitart.no2.objects;

import org.dizitart.no2.*;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.internals.NitriteMapper;
import org.dizitart.no2.util.Iterables;

import java.util.Iterator;
import java.util.List;

import static org.dizitart.no2.exceptions.ErrorCodes.VE_PROJECT_NULL_PROJECTION;
import static org.dizitart.no2.exceptions.ErrorMessage.OBJ_REMOVE_ON_OBJECT_ITERATOR_NOT_SUPPORTED;
import static org.dizitart.no2.exceptions.ErrorMessage.errorMessage;
import static org.dizitart.no2.util.DocumentUtils.emptyDocument;
import static org.dizitart.no2.util.ValidationUtils.notNull;

/**
 * @author Anindya Chatterjee
 * */
class ObjectCursor<T> implements Cursor<T> {
    private org.dizitart.no2.Cursor cursor;
    private NitriteMapper nitriteMapper;
    private Class<T> type;
    private Iterator<T> cursorIterator;

    ObjectCursor(NitriteMapper nitriteMapper, org.dizitart.no2.Cursor cursor, Class<T> type) {
        this.nitriteMapper = nitriteMapper;
        this.cursor = cursor;
        this.type = type;
        this.cursorIterator = new ObjectCursorIterator(cursor.iterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> RecordIterable<P> project(Class<P> projectionType) {
        notNull(projectionType, errorMessage("projection can not be null", VE_PROJECT_NULL_PROJECTION));
        Document dummyDoc = emptyDocument(nitriteMapper, projectionType);
        return new ProjectedObjectIterable<>(nitriteMapper, cursor.project(dummyDoc), projectionType);
    }

    @Override
    public boolean hasMore() {
        return cursor.hasMore();
    }

    @Override
    public int size() {
        return cursor.size();
    }

    @Override
    public int totalCount() {
        return cursor.totalCount();
    }

    @Override
    public T firstOrDefault() {
        T item = Iterables.firstOrDefault(this);
        reset();
        return item;
    }

    @Override
    public List<T> toList() {
        List<T> list = Iterables.toList(this);
        reset();
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return cursorIterator;
    }

    @Override
    public void reset() {
        cursor.reset();
        cursorIterator = new ObjectCursorIterator(cursor.iterator());
    }

    private class ObjectCursorIterator implements Iterator<T> {
        private Iterator<Document> documentIterator;

        ObjectCursorIterator(Iterator<Document> documentIterator) {
            this.documentIterator = documentIterator;
        }

        @Override
        public boolean hasNext() {
            return documentIterator.hasNext();
        }

        @Override
        public T next() {
            Document document = documentIterator.next();
            if (document != null) {
                return nitriteMapper.asObject(document, type);
            }
            return null;
        }

        @Override
        public void remove() {
            throw new InvalidOperationException(OBJ_REMOVE_ON_OBJECT_ITERATOR_NOT_SUPPORTED);
        }
    }
}
