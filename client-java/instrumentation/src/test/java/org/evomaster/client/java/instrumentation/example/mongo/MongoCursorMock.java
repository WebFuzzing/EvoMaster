package org.evomaster.client.java.instrumentation.example.mongo;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;

import java.util.Iterator;

public class MongoCursorMock<Result> implements MongoCursor<Result> {

    private final Iterator<Result> iterator;

    public MongoCursorMock(Iterator<Result> iterator) {
        this.iterator = iterator;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Result next() {
        return iterator.next();
    }

    @Override
    public Result tryNext() {
        return null;
    }

    @Override
    public ServerCursor getServerCursor() {
        return null;
    }

    @Override
    public ServerAddress getServerAddress() {
        return null;
    }
}
