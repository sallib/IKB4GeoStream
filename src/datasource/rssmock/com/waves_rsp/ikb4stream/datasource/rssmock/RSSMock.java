package com.waves_rsp.ikb4stream.datasource.rssmock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.waves_rsp.ikb4stream.core.datasource.model.IDataProducer;
import com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector;
import com.waves_rsp.ikb4stream.core.model.Event;
import com.waves_rsp.ikb4stream.core.model.LatLong;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import com.waves_rsp.ikb4stream.core.util.Geocoder;
import com.waves_rsp.ikb4stream.core.util.LanguageDetection;
import com.waves_rsp.ikb4stream.core.util.nlp.OpenNLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;


public class RSSMock implements IProducerConnector {
    /**
     * Properties of this module
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class, String)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(RSSMock.class, "resources/datasource/rssmock/config.properties");
    /**
     * Logger used to log all information in this module
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSSMock.class);
    /**
     * Source name of corresponding {@link Event}
     *
     * @see TwitterMock#getEventFromJson(ObjectNode)
     */

    /**
     * Single instance of {@link OpenNLP} per each Thread
     *
     * @see RSSMock#geocodeRSS(String, String, OpenNLP.langOptions)
     */
    private final OpenNLP openNLP = OpenNLP.getOpenNLP(Thread.currentThread());

    private final LanguageDetection languageDetection = LanguageDetection.getLanguageDetection(Thread.currentThread());

    private List<File> mockfiles= new ArrayList<>();
    private File directory ;

    /**
     * Override default constructor
     */
    public RSSMock() {
        try {
            this.directory = new File(PROPERTIES_MANAGER.getProperty("RSSMock.directory"));
            File[] files = directory.listFiles();
            Arrays.asList(files).forEach(f->mockfiles.add(f));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid configuration [] ", e);
            throw new IllegalStateException("Invalid configuration");
        }
    }

    @Override
    public void load(IDataProducer dataProducer) {
        Objects.requireNonNull(dataProducer);
        int rssCount = mockfiles.size();
        for (int i = 0; i < rssCount - 1; i++) {
            try {
                File file = mockfiles.get(i);
                String source = "MOCK_"+mockfiles.get(i).getName();
                LOGGER.info("\n*********************************************\n" + "************* " + source + " " + i + "\n*********************************************\n");
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(file));
                Date currentTime = Date.from(Instant.now());
                feed.getEntries().stream()

                        .forEach(entry -> {

                            Date startDate = (entry.getPublishedDate() != null) ? entry.getPublishedDate() : currentTime;
                            //check that RSS is not already in database

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
                                LOGGER.info("Push event "+event);
                                dataProducer.push(event);
                            }
                        });
            } catch (MalformedURLException e) {
                LOGGER.error("Invalid configuration [] ", e);
                throw new IllegalStateException("Invalid configuration");
            } catch (IOException | FeedException e) {
                LOGGER.error("Can't parse RSS [] ", e);
            }
        }
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
     * Select a list of location from a RSS with the NER OpenNLP algorithme.
     * Then, geolocalize the first location found with the geocoder Nominatim (OSM)
     *
     * @param text to analyze, source
     * @return a latLong coordinates
     * @see RSSMock#openNLP
     */
    private LatLong geocodeRSS(String text, String source, OpenNLP.langOptions lang) {
        long start = System.currentTimeMillis();

        List<String> locations = openNLP.applyNLPner(text, OpenNLP.nerOptions.LOCATION, lang);
        if (!locations.isEmpty()) {
            long time = System.currentTimeMillis() - start;
            LOGGER.info("geocode RSS at " + locations.get(0));
            return Geocoder.geocode(locations.get(0)).getLatLong();
        }
        LOGGER.info("Can't find location");
        return new LatLong(0, 0);
    }

    public List<String> getRSSMockSources(){
        List<String> sources = new ArrayList<>();
        mockfiles.forEach(f-> sources.add("MOCK_" + f.getName()));
        return sources;
    }
    public boolean isActive() {
        try {
            return Boolean.valueOf(PROPERTIES_MANAGER.getProperty("RSSMock.enable"));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }


    public static void main(String[] args) {
        RSSMock mock = new RSSMock();
        mock.load(event -> {
            // Do nothing
        });
    }
}
