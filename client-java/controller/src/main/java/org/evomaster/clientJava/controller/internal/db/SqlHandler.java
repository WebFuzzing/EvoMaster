package org.evomaster.clientJava.controller.internal.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class SqlHandler {

    private final List<String> buffer;
    private final List<Double> distances;

    public SqlHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
    }

    public void handle(String sql) {
        Objects.requireNonNull(sql);

        buffer.add(sql);
    }


    public void reset() {
        buffer.clear();
        distances.clear();
    }

    public List<Double> getDistances() {

        buffer.stream()
                .filter(sql -> isSelect(sql))
                .forEach(sql -> {
                    double dist = computeDistance(sql);
                    distances.add(dist);
                });
        buffer.clear();

        return distances;
    }

    private boolean isSelect(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }

    private Double computeDistance(String select) {

        //TODO

        return -1d;
    }
}
