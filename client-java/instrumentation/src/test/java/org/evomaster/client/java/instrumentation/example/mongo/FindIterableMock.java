package org.evomaster.client.java.instrumentation.example.mongo;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Collation;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FindIterableMock<Result> implements FindIterable<Result> {
    private List<Result> elements = new ArrayList<>();

    public void add(Result document) {
        elements.add(document);
    }

    @Override
    public FindIterable<Result> filter(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> limit(int i) {
        return null;
    }

    @Override
    public FindIterable<Result> skip(int i) {
        return null;
    }

    @Override
    public FindIterable<Result> maxTime(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public FindIterable<Result> maxAwaitTime(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public FindIterable<Result> projection(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> sort(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> noCursorTimeout(boolean b) {
        return null;
    }

    @Override
    public FindIterable<Result> oplogReplay(boolean b) {
        return null;
    }

    @Override
    public FindIterable<Result> partial(boolean b) {
        return null;
    }

    @Override
    public FindIterable<Result> cursorType(CursorType cursorType) {
        return null;
    }

    @Override
    public MongoCursorMock<Result> iterator() {
        return new MongoCursorMock(elements.iterator());
    }

    @Override
    public MongoCursor<Result> cursor() {
        return null;
    }

    @Override
    public Result first() {
        return null;
    }

    @Override
    public <U> MongoIterable<U> map(Function<Result, U> function) {
        return null;
    }

    @Override
    public <A extends Collection<? super Result>> A into(A objects) {
        return null;
    }

    @Override
    public FindIterable<Result> batchSize(int i) {
        return null;
    }

    @Override
    public FindIterable<Result> collation(Collation collation) {
        return null;
    }

    @Override
    public FindIterable<Result> comment(String s) {
        return null;
    }


    @Override
    public FindIterable<Result> hint(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> hintString(String s) {
        return null;
    }

    @Override
    public FindIterable<Result> max(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> min(Bson bson) {
        return null;
    }

    @Override
    public FindIterable<Result> returnKey(boolean b) {
        return null;
    }

    @Override
    public FindIterable<Result> showRecordId(boolean b) {
        return null;
    }

    @Override
    public FindIterable<Result> allowDiskUse(Boolean aBoolean) {
        return null;
    }

    @Override
    public Document explain() {
        return null;
    }

    @Override
    public Document explain(ExplainVerbosity explainVerbosity) {
        return null;
    }

    @Override
    public <E> E explain(Class<E> aClass) {
        return null;
    }

    @Override
    public <E> E explain(Class<E> aClass, ExplainVerbosity explainVerbosity) {
        return null;
    }
}

