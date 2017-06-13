/*
 * Copyright (C) 2017 ikb4stream team
 * ikb4stream is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * ikb4stream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 *
 */

package com.waves_rsp.ikb4stream.consumer.database;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.TextSearchOptions;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import com.waves_rsp.ikb4stream.core.communication.DatabaseReaderCallback;
import com.waves_rsp.ikb4stream.core.communication.model.IDatabaseReaderB;
import com.waves_rsp.ikb4stream.core.communication.model.IDatabaseReader;
import com.waves_rsp.ikb4stream.core.communication.model.Request;
import com.waves_rsp.ikb4stream.core.metrics.MetricsLogger;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

/**
 * DatabaseReader class reads data (Events) from mongodb database
 *
 * @author ikb4stream
 * @version 1.0
 * @see IDatabaseReaderB
 */
public class DatabaseReader implements IDatabaseReader {
    /**
     * Properties of this class
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(DatabaseReader.class);
    /**
     * Object to add metrics from this class
     *
     * @see MetricsLogger#getMetricsLogger()
     * @see MetricsLogger#log(String, long)
     */
    private static final MetricsLogger METRICS_LOGGER = MetricsLogger.getMetricsLogger();
    /**
     * Logger used to log all information in this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseReader.class);
    /**
     * Single instance of {@link DatabaseReader}
     *
     * @see DatabaseReader#getEvent(Request, DatabaseReaderCallback)
     * @see DatabaseReader#getInstance()
     */
    private static final DatabaseReader DATABASE_READER = new DatabaseReader();
    /**
     * Object use to read Document from MongoDb
     *
     * @see DatabaseReader#getEvent(Request, DatabaseReaderCallback)
     */
    private final MongoCollection<Document> mongoCollection;
    /**
     * Result limit of request
     *
     * @see DatabaseReader#getEvent(Request, DatabaseReaderCallback)
     */
    private final int limit;

    /**
     * The constructor of {@link DatabaseReader}
     * This class is a singleton
     *
     * @see DatabaseReader#checkConfiguration()
     * @see DatabaseReader#PROPERTIES_MANAGER
     * @see DatabaseReader#mongoCollection
     * @see DatabaseReader#limit
     */
    private DatabaseReader() {
        try {
            checkConfiguration();
            final MongoClient mongoClient = MongoClients.create(PROPERTIES_MANAGER.getProperty("database.host"));
            final MongoDatabase mongoDatabase = mongoClient.getDatabase(PROPERTIES_MANAGER.getProperty("database.datasource"));
            this.mongoCollection = mongoDatabase.getCollection(PROPERTIES_MANAGER.getProperty("database.collection"));

            this.mongoCollection.createIndex(Indexes.text("description"), callbackWhenFinished);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        }
        int tmp = 50000;
        try {
            tmp = Integer.parseInt(PROPERTIES_MANAGER.getProperty("database.limit"));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Use default database.limit");
        }
        this.limit = tmp;
        LOGGER.info("DatabaseReader has been instantiate");
    }

    /**
     * Check configuration of Database
     *
     * @see DatabaseReader#PROPERTIES_MANAGER
     */
    private static void checkConfiguration() {
        PROPERTIES_MANAGER.getProperty("database.host");
        PROPERTIES_MANAGER.getProperty("database.datasource");
        PROPERTIES_MANAGER.getProperty("database.collection");
    }

    /**
     * Get instance of Singleton DatabaseReader
     *
     * @return an instance of {@link DatabaseReader}
     * @see DatabaseReader#DATABASE_READER
     */
    public static DatabaseReader getInstance() {
        return DATABASE_READER;
    }

    /**
     * This method requests events from mongodb database and filters from data coming to the request object in parameter
     *
     * @param request  Request to apply to Mongo
     * @param callback Callback method call after select operation
     * @see DatabaseReader#limit
     * @see DatabaseReader#mongoCollection
     */
    @Override
    public void getEvent(Request request, DatabaseReaderCallback callback) {
        final long start = System.currentTimeMillis();

        Bson filter = createFilter(request);
        this.mongoCollection
                .find(filter)
                .limit(limit)
                .into(new ArrayList<Document>(),
                        (result, t) -> {
                            long time = System.currentTimeMillis() - start;
                            METRICS_LOGGER.log("time_dbreader", time);
                            callback.onResult(
                                    t,
                                    "[" + result.stream().map(Document::toJson).collect(Collectors.joining(", ")) + "]"
                            );
                        });
    }

    @Override
    public void deleteEvent(String id){
        LOGGER.info("DELETE EVENT");
        StringBuilder sb = new StringBuilder();
        sb.append("ObjectId(").append('"').append(id).append('"').append(")");
        this.mongoCollection.find(eq("_id", sb.toString())).first(printDocument);


        StringBuilder sb2 = new StringBuilder();
        sb2.append( "{ ").append('"').append("$oid").append('"').append(" : ").append('"').append(id).append('"').append(" }");
        this.mongoCollection.find(eq("_id", sb2.toString())).first(printDocument);
        //this.mongoCollection.deleteOne(eq("_id", id), (result, t) -> System.out.println(result.getDeletedCount()));
  }

    private Bson createFilter(Request request) {
        Objects.requireNonNull(request);

        Bson filterSearch = text(request.getSearch(), new TextSearchOptions().caseSensitive(false));
        Bson filterStartDate = lte("start", request.getEnd().getTime());
        Bson filterEndDate = gte("end", request.getStart().getTime());
        Bson filterGeo = getFilterGeo(request);
        if (request.getSearch().isEmpty() && filterGeo == null) {
            return and(filterStartDate, filterEndDate);
        } else if (request.getSearch().isEmpty()) {
            return and(filterStartDate, filterEndDate, filterGeo);
        }else if (filterGeo == null){
            return and(filterStartDate, filterEndDate, filterSearch);
        } else {
            return and(filterStartDate, filterEndDate, filterSearch, filterGeo);
        }

    }

    private Bson getFilterGeo(Request request){
        if (request.getBoundingBox() != null) {
            List<Position> polygon = Arrays.stream(request.getBoundingBox().getLatLongs())
                    .map(l -> new Position(l.getLongitude(), l.getLatitude()))
                    .collect(Collectors.toList());
            return geoIntersects("location", new Polygon(polygon));
        }
        return null;
    }

    SingleResultCallback<String> callbackWhenFinished = new SingleResultCallback<String>() {
        @Override
        public void onResult(final String result, final Throwable t) {
            LOGGER.info("Indexes description of events finish !  " + result);
        }
    };

    SingleResultCallback<Document> printDocument = new SingleResultCallback<Document>() {
        @Override
        public void onResult(final Document document, final Throwable t) {
            LOGGER.info(document.toJson());
        }
    };
}