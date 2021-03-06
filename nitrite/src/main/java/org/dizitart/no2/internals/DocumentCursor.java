/*
 *
 * Copyright 2017 Nitrite author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dizitart.no2.internals;

import org.dizitart.no2.*;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.exceptions.ValidationException;
import org.dizitart.no2.store.NitriteMap;
import org.dizitart.no2.util.Iterables;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static org.dizitart.no2.exceptions.ErrorMessage.PROJECTION_WITH_NOT_NULL_VALUES;
import static org.dizitart.no2.exceptions.ErrorMessage.REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED;

/**
 * @author Anindya Chatterjee.
 */
class DocumentCursor implements Cursor {
    private final Collection<NitriteId> resultSet;
    private final NitriteMap<NitriteId, Document> underlyingMap;
    private boolean hasMore;
    private int totalCount;
    private FindResult findResult;

    DocumentCursor(FindResult findResult) {
        if (findResult.getIdSet() != null) {
            resultSet = findResult.getIdSet();
        } else {
            resultSet = new TreeSet<>();
        }
        this.underlyingMap = findResult.getUnderlyingMap();
        this.hasMore = findResult.isHasMore();
        this.totalCount = findResult.getTotalCount();
        this.findResult = findResult;
    }

    @Override
    public RecordIterable<Document> project(Document projection) {
        validateProjection(projection);
        return new ProjectedDocumentIterable(projection, findResult);
    }

    @Override
    public RecordIterable<Document> join(Cursor cursor, Lookup lookup) {
        return new JoinedDocumentIterable(findResult, cursor, lookup);
    }

    @Override
    public Iterator<Document> iterator() {
        return new DocumentCursorIterator();
    }

    @Override
    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public int size() {
        return resultSet.size();
    }

    @Override
    public int totalCount() {
        return totalCount;
    }

    @Override
    public Document firstOrDefault() {
        return Iterables.firstOrDefault(this);
    }

    @Override
    public List<Document> toList() {
        return Iterables.toList(this);
    }

    private class DocumentCursorIterator implements Iterator<Document> {
        private Iterator<NitriteId> iterator;

        DocumentCursorIterator() {
            iterator = resultSet.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Document next() {
            NitriteId next = iterator.next();
            return underlyingMap.get(next);
        }

        @Override
        public void remove() {
            throw new InvalidOperationException(REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED);
        }
    }

    private void validateProjection(Document projection) {
        for (KeyValuePair kvp : projection) {
            validateKeyValuePair(kvp);
        }
    }

    private void validateKeyValuePair(KeyValuePair kvp) {
        if (kvp.getValue() != null) {
            if (!(kvp.getValue() instanceof Document)) {
                throw new ValidationException(PROJECTION_WITH_NOT_NULL_VALUES);
            } else {
                validateProjection((Document) kvp.getValue());
            }
        }
    }
}
