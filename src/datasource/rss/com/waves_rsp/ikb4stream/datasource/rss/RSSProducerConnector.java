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

package com.waves_rsp.ikb4stream.datasource.rss;

import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.waves_rsp.ikb4stream.core.datasource.model.IDataProducer;
import com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector;
import com.waves_rsp.ikb4stream.core.metrics.MetricsLogger;
import com.waves_rsp.ikb4stream.core.model.Event;
import com.waves_rsp.ikb4stream.core.model.LatLong;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import com.waves_rsp.ikb4stream.core.util.Geocoder;
import com.waves_rsp.ikb4stream.core.util.LanguageDetection;
import com.waves_rsp.ikb4stream.core.util.nlp.OpenNLP;
import com.waves_rsp.ikb4stream.core.util.nlp.OpenNLP.nerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Get data flow from RSS
 *
 * @author ikb4stream
 * @version 1.0
 * @see com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector
 */
public class RSSProducerConnector implements IProducerConnector {
    /**
     * Properties of this module
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class, String)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(RSSProducerConnector.class, "resources/datasource/rss/config.properties");
    /**
     * Logger used to log all information in this module
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSSProducerConnector.class);
    /**
     * Object to add metrics from this class
     *
     * @see MetricsLogger#log(String, long)
     * @see MetricsLogger#getMetricsLogger()
     */
    private static final MetricsLogger METRICS_LOGGER = MetricsLogger.getMetricsLogger();
    /**
     * Single instance of {@link OpenNLP} per each Thread
     *
     * @see RSSProducerConnector#geocodeRSS(String, String, OpenNLP.langOptions)
     */
    private final OpenNLP openNLP = OpenNLP.getOpenNLP(Thread.currentThread());
    /**
     * Source name of corresponding {@link Event}
     *
     * @see RSSProducerConnector#geocodeRSS(String, String, OpenNLP.langOptions)
     * @see RSSProducerConnector#load(IDataProducer)
     */
    private final String[] sources;
    /**
     * Interval time between two batch
     *
     * @see RSSProducerConnector#load(IDataProducer)
     */
    private final int interval;
    /**
     * @see RSSProducerConnector#load(IDataProducer)
     */
    private final String[] urls;

    private final LanguageDetection languageDetection = LanguageDetection.getLanguageDetection(Thread.currentThread());

    private final Date lastLoading;
    private final DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss ZZZ yyyy", Locale.ENGLISH);
    /**
     * Public constructor to init variable from {@link RSSProducerConnector#PROPERTIES_MANAGER}
     *
     * @throws IllegalStateException if invalid value in configuration file
     * @see RSSProducerConnector#sources
     * @see RSSProducerConnector#interval
     * @see RSSProducerConnector#urls
     */
    public RSSProducerConnector() {
        try {
            this.sources = PROPERTIES_MANAGER.getProperty("RSSProducerConnector.source").split(",");
            this.interval = Integer.parseInt(PROPERTIES_MANAGER.getProperty("RSSProducerConnector.interval"));
            this.lastLoading = df.parse(PROPERTIES_MANAGER.getProperty("RSSProducerConnector.lastLoading"));
            this.urls = PROPERTIES_MANAGER.getProperty("RSSProducerConnector.url").split(",");
        } catch (IllegalArgumentException | ParseException e) {
            LOGGER.error("Invalid configuration [] ", e);
            throw new IllegalStateException("Invalid configuration");
        }
    }

    /**
     * Listen {@link Event} from RSS
     *
     * @param dataProducer {@link IDataProducer} contains the data queue
     * @throws NullPointerException if dataProducer is null
     * @see RSSProducerConnector#sources
     * @see RSSProducerConnector#urls
     * @see RSSProducerConnector#interval
     */
    @Override
    public void load(IDataProducer dataProducer) {
        Objects.requireNonNull(dataProducer);
        final int rssCount = countSources();
        final boolean[] first = {true};
        final Date[] lastTime = {Date.from(Instant.now())};
        System.out.println("sleeping time : " + (this.interval/1000)/60);
        while (!Thread.currentThread().isInterrupted()) {
            LOGGER.info("BEFORE : LastLoading property : " + PROPERTIES_MANAGER.getProperty("RSSProducerConnector.lastLoading") + "\n");
            PROPERTIES_MANAGER.setProperty("RSSProducerConnector.lastLoading", String.valueOf(Date.from(Instant.now())));
            LOGGER.info("AFTER : LastLoading property : " + PROPERTIES_MANAGER.getProperty("RSSProducerConnector.lastLoading") + "\n");
            for (int i = 0; i < rssCount; i++) {
                LOGGER.info("\n*********************************************\n" + "*************" + sources[i] +  i  +"\n*********************************************\n" );
                first[0] = true;
                try {
                    URL url = new URL(urls[i]);
                    String source = sources[i];

                    long start = System.currentTimeMillis(); //metrics
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(new XmlReader(url));
                    Date currentTime = Date.from(Instant.now());
                    feed.getEntries().stream()
                            .filter(entry -> first[0] || entry.getPublishedDate().after(lastTime[0]))
                            .forEach(entry -> {
                                lastTime[0] = currentTime;
                                Date startDate = (entry.getPublishedDate() != null) ? entry.getPublishedDate() : currentTime;
                                //check that RSS is not already in database
                                if (startDate.after(lastLoading) ) {
                                    String description = "";
                                    if (entry.getDescription() != null) {
                                        description = (entry.getDescription().getValue() != null) ? entry.getDescription().getValue() : "";
                                    }
                                    String completeDesc = entry.getTitle() + "\\n" + description + "\\nVoir plus: " + entry.getLink();
                                    GeoRSSModule module = GeoRSSUtils.getGeoRSS(entry);
                                    String tmpPost = description;
                                    if (tmpPost.length() > 1250) {
                                        tmpPost = description.substring(0, 1250);
                                    }
                                    OpenNLP.langOptions lang = languageDetection.detectLanguage(tmpPost);
                                    LatLong latLong = getLatLong(module, completeDesc, source, lang);
                                    if (latLong != null) {
                                        Event event = new Event(latLong, startDate, currentTime, completeDesc, source, lang);
                                        dataProducer.push(event);
                                    }
                                }});
                    first[0] = false;
                    long time = System.currentTimeMillis() - start;
                    METRICS_LOGGER.log("time_process_" + source, time);
                    if (i == this.sources.length -1) {
                        LOGGER.info("*** RSS loading is finish for this interval. Next time in " + ((this.interval/1000)/60)/60 + " hours ***\n");
                        Thread.sleep(interval);
                    }
                } catch (MalformedURLException e) {
                    LOGGER.error("Invalid configuration [] ", e);
                    throw new IllegalStateException("Invalid configuration");
                } catch (IOException | FeedException e) {
                    LOGGER.error("Can't parse RSS [] ", e);
                    if (i == this.sources.length -1) {
                        LOGGER.info("*** RSS loading is finish for this interval. Next time in " + ((this.interval/1000)/60)/60 + " hours ***\n");
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    private int countSources() {
        int count = sources.length;
        if (count != urls.length) {
            LOGGER.error("Invalid configuration : number of sources is different of number of urls");
            throw new IllegalStateException("Invalid configuration");
        }
        return count;
    }

    /**
     * Getting a {@link LatLong} from a GeoRSSModule or a description
     *
     * @param module GeoRSSModule that represent a {@link LatLong}
     * @param desc   Description of {@link Event}
     * @return {@link LatLong} if found something or null
     * @see LatLong
     */
    private LatLong getLatLong(GeoRSSModule module, String desc, String source, OpenNLP.langOptions lang) {
        if (module != null) {
            return new LatLong(module.getPosition().getLatitude(), module.getPosition().getLongitude());
        } else if (desc != null) {
            return geocodeRSS(desc, source, lang);
        }
        return null;
    }



    /**
     * Check if this jar is active
     *
     * @return true if it should be started
     * @see RSSProducerConnector#PROPERTIES_MANAGER
     */
    @Override
    public boolean isActive() {
        try {
            return Boolean.valueOf(PROPERTIES_MANAGER.getProperty("RSSProducerConnector.enable"));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Select a list of location from a RSS with the NER OpenNLP algorithme.
     * Then, geolocalize the first location found with the geocoder Nominatim (OSM)
     *
     * @param text to analyze, source
     * @return a latLong coordinates
     * @see RSSProducerConnector#openNLP
     */
    private LatLong geocodeRSS(String text, String source, OpenNLP.langOptions lang) {
        long start = System.currentTimeMillis();

        List<String> locations = openNLP.applyNLPner(text, nerOptions.LOCATION, lang);
        if (!locations.isEmpty()) {
            long time = System.currentTimeMillis() - start;
            METRICS_LOGGER.log("time_geocode_" + source, time);
            LOGGER.info("geocode RSS at " + locations.get(0));
            return Geocoder.geocode(locations.get(0)).getLatLong();
        }
        LOGGER.info("Can't find location");
        return new LatLong(0,0);
    }


    public static void main(String[] args){
        System.out.println(Date.from(Instant.now()));
        RSSProducerConnector rss = new RSSProducerConnector();

        Thread t = new Thread(() -> rss.load(event -> {

        }));
        t.start();
    }

}
