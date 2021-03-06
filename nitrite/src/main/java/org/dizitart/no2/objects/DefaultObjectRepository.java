package org.dizitart.no2.objects;

import lombok.val;
import org.dizitart.no2.*;
import org.dizitart.no2.Index;
import org.dizitart.no2.event.ChangeListener;
import org.dizitart.no2.exceptions.InvalidIdException;
import org.dizitart.no2.exceptions.NotIdentifiableException;
import org.dizitart.no2.exceptions.ValidationException;
import org.dizitart.no2.internals.NitriteMapper;
import org.dizitart.no2.meta.Attributes;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.dizitart.no2.Constants.DOC_ID;
import static org.dizitart.no2.IndexOptions.indexOptions;
import static org.dizitart.no2.UpdateOptions.updateOptions;
import static org.dizitart.no2.exceptions.ErrorCodes.*;
import static org.dizitart.no2.exceptions.ErrorMessage.*;
import static org.dizitart.no2.util.ObjectUtils.createUniqueFilter;
import static org.dizitart.no2.util.ObjectUtils.extractIndices;
import static org.dizitart.no2.util.ObjectUtils.getIdField;
import static org.dizitart.no2.util.StringUtils.isNullOrEmpty;
import static org.dizitart.no2.util.ValidationUtils.notNull;

/**
 * A default implementation of {@link ObjectRepository}.
 *
 * @since 1.0
 * @author Anindya Chatterjee.
 */
class DefaultObjectRepository<T> implements ObjectRepository<T> {
    private NitriteCollection collection;
    private Class<T> type;
    private NitriteMapper nitriteMapper;
    private Field idField;

    DefaultObjectRepository(Class<T> type, NitriteCollection collection,
                            NitriteContext nitriteContext) {
        this.type = type;
        this.collection = collection;
        initRepository(nitriteContext);
    }

    @Override
    public void createIndex(String field, IndexOptions indexOptions) {
        validateCollection();
        notNull(field, errorMessage("field can not be null", VE_OBJ_CREATE_INDEX_NULL_FIELD));
        collection.createIndex(field, indexOptions);
    }

    @Override
    public void rebuildIndex(String field, boolean async) {
        validateCollection();
        collection.rebuildIndex(field, async);
    }

    @Override
    public Collection<Index> listIndices() {
        validateCollection();
        return collection.listIndices();
    }

    @Override
    public boolean hasIndex(String field) {
        validateCollection();
        return collection.hasIndex(field);
    }

    @Override
    public boolean isIndexing(String field) {
        validateCollection();
        return collection.isIndexing(field);
    }

    @Override
    public void dropIndex(String field) {
        validateCollection();
        collection.dropIndex(field);
    }

    @Override
    public void dropAllIndices() {
        validateCollection();
        collection.dropAllIndices();
    }

    @SafeVarargs
    @Override
    public final WriteResult insert(T object, T... others) {
        validateCollection();
        return collection.insert(asDocument(object, true), asDocuments(others));
    }

    @Override
    public WriteResult insert(T[] objects) {
        validateCollection();
        return collection.insert(asDocuments(objects));
    }

    @Override
    public WriteResult update(T element) {
        return update(element, false);
    }

    @Override
    public WriteResult update(T element, boolean upsert) {
        if (idField == null) {
            throw new NotIdentifiableException(OBJ_UPDATE_FAILED_AS_NO_ID_FOUND);
        }
        return update(createUniqueFilter(element, idField), element, updateOptions(upsert));
    }

    @Override
    public WriteResult update(ObjectFilter filter, T update) {
        return update(filter, update, new UpdateOptions());
    }

    @Override
    public WriteResult update(ObjectFilter filter, T update, UpdateOptions updateOptions) {
        validateCollection();
        notNull(update, errorMessage("update can not be null", VE_OBJ_UPDATE_NULL_OBJECT));
        notNull(updateOptions, errorMessage("updateOptions can not be null", VE_OBJ_UPDATE_NULL_UPDATE_OPTIONS));

        Document updateDocument = asDocument(update, false);
        filterKeys(updateDocument);
        return collection.update(prepare(filter), updateDocument, updateOptions);
    }

    @Override
    public WriteResult update(ObjectFilter filter, Document update) {
        return update(filter, update, false);
    }

    @Override
    public WriteResult update(ObjectFilter filter, Document update, boolean justOnce) {
        validateCollection();
        notNull(update, errorMessage("update can not be null", VE_OBJ_UPDATE_NULL_DOCUMENT));

        filterKeys(update);
        return collection.update(prepare(filter), update, updateOptions(false, justOnce));
    }

    @Override
    public WriteResult remove(T element) {
        if (idField == null) {
            throw new NotIdentifiableException(OBJ_REMOVE_FAILED_AS_NO_ID_FOUND);
        }
        return remove(createUniqueFilter(element, idField));
    }

    @Override
    public WriteResult remove(ObjectFilter filter) {
        validateCollection();
        return remove(prepare(filter), new RemoveOptions());
    }

    @Override
    public WriteResult remove(ObjectFilter filter, RemoveOptions removeOptions) {
        validateCollection();
        return collection.remove(prepare(filter), removeOptions);
    }

    @Override
    public Cursor<T> find() {
        validateCollection();
        return new ObjectCursor<>(nitriteMapper, collection.find(), type);
    }

    @Override
    public Cursor<T> find(ObjectFilter filter) {
        validateCollection();
        notNull(filter, errorMessage("filter can not be null", VE_OBJ_FIND_NULL_FILTER));
        return new ObjectCursor<>(nitriteMapper,
                collection.find(prepare(filter)), type);
    }

    @Override
    public Cursor<T> find(FindOptions findOptions) {
        validateCollection();
        return new ObjectCursor<>(nitriteMapper,
                collection.find(findOptions), type);
    }

    @Override
    public Cursor<T> find(ObjectFilter filter, FindOptions findOptions) {
        validateCollection();
        notNull(filter, errorMessage("filter can not be null", VE_OBJ_FIND_WITH_OPTIONS_NULL_FILTER));
        return new ObjectCursor<>(nitriteMapper,
                collection.find(prepare(filter), findOptions), type);
    }

    @Override
    public T getById(NitriteId nitriteId) {
        validateCollection();
        Document document = new Document(collection.getById(nitriteId));
        document.remove(DOC_ID);
        return nitriteMapper.asObject(document, type);
    }

    @Override
    public void drop() {
        validateCollection();
        collection.drop();
    }

    @Override
    public boolean isDropped() {
        validateCollection();
        return collection.isDropped();
    }

    @Override
    public String getName() {
        return collection.getName();
    }

    @Override
    public long size() {
        validateCollection();
        return collection.size();
    }

    @Override
    public boolean isClosed() {
        validateCollection();
        return collection.isClosed();
    }

    @Override
    public void close() {
        validateCollection();
        collection.close();
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public NitriteCollection getDocumentCollection() {
        return collection;
    }

    @Override
    public Attributes getAttributes() {
        return collection.getAttributes();
    }

    @Override
    public void setAttributes(Attributes attributes) {
        collection.setAttributes(attributes);
    }

    @Override
    public void register(ChangeListener listener) {
        collection.register(listener);
    }

    @Override
    public void deregister(ChangeListener listener) {
        collection.deregister(listener);
    }

    private void validateCollection() {
        if (collection == null) {
            throw new ValidationException(REPOSITORY_NOT_INITIALIZED);
        }
    }

    private Document asDocument(T object, boolean idCheck) {
        Document document = nitriteMapper.asDocument(object);
        if (idField != null && idCheck) {
            Object idValue = document.get(idField.getName());
            if (idValue == null) {
                throw new InvalidIdException(ID_CAN_NOT_BE_NULL);
            }
            if (idValue instanceof String && isNullOrEmpty((String) idValue)) {
                throw new InvalidIdException(ID_VALUE_CAN_NOT_BE_EMPTY_STRING);
            }
        }
        return document;
    }

    private Document[] asDocuments(T[] others) {
        if (others == null || others.length == 0) return null;
        Document[] documents = new Document[others.length];
        for (int i = 0; i < others.length; i++) {
            documents[i] = asDocument(others[i], true);
        }
        return documents;
    }

    private void initRepository(NitriteContext nitriteContext) {
        nitriteMapper = nitriteContext.getNitriteMapper();
        createIndexes();
    }

    private void createIndexes() {
        validateCollection();
        val indexes = extractIndices(nitriteMapper, type);
        for (val idx : indexes) {
            if (!collection.hasIndex(idx.value())) {
                collection.createIndex(idx.value(), indexOptions(idx.type(), false));
            }
        }

        idField = getIdField(nitriteMapper, type);
        if (idField != null) {
            if (!collection.hasIndex(idField.getName())) {
                collection.createIndex(idField.getName(), indexOptions(IndexType.Unique));
            }
        }
    }

    private ObjectFilter prepare(ObjectFilter objectFilter) {
        if (objectFilter != null) {
            objectFilter.setNitriteMapper(nitriteMapper);
            return objectFilter;
        }
        return null;
    }

    private void filterKeys(Document document) {
        document.remove(DOC_ID);

        Document doc = new Document(document);
        for (KeyValuePair pair : doc) {
            if (pair.getValue() == null) {
                document.remove(pair.getKey());
            }
        }
    }
}
