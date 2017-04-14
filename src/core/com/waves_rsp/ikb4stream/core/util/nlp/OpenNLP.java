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

package com.waves_rsp.ikb4stream.core.util.nlp;

import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.SimpleLemmatizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author ikb4stream
 * @version 1.0
 */
public class OpenNLP {
    /**
     * Properties of this class
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(OpenNLP.class);
    /**
     * Logger used to log all information in this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNLP.class);
    /**
     * Store unique instance per Thread of {@link OpenNLP}
     *
     * @see OpenNLP#getOpenNLP(Thread)
     */
    private static final Map<Thread, OpenNLP> INSTANCES = new HashMap<>();
    /**
     * Load lemmatizer model
     *
     * @see OpenNLP#lemmatize(String, langOptions)
     */
    private final DictionaryLemmatizer lemmatizerFr;
    private final DictionaryLemmatizer lemmatizerEn;
    /**
     * Use to do sentence detection
     *
     * @see OpenNLP#detectSentences(String, langOptions)
     */
    private final SentenceDetectorME detectorFr;
    private final SentenceDetectorME detectorEn;
    /**
     * Use to apply person name finder
     *
     * @see OpenNLP#findPersonName(String[], langOptions)
     */
    private final NameFinderME nameFinderPersFr;
    private final NameFinderME nameFinderPersEn;
    /**
     * Use to apply organization name finder
     *
     * @see OpenNLP#findOrganizationName(String[], langOptions)
     */
    private final NameFinderME nameFinderOrgFr;
    private final NameFinderME nameFinderOrgEn;
    /**
     * Use to apply location name finder
     *
     * @see OpenNLP#findLocationName(String[], langOptions)
     */
    private final NameFinderME nameFinderLocFr;
    private final NameFinderME nameFinderLocEn;
    /**
     * Use to apply tokenization
     *
     * @see OpenNLP#learnableTokenize(String, langOptions)
     */
    private final Tokenizer tokenizerFr;
    private final Tokenizer tokenizerEn;
    /**
     * Use to apply part-of-speech tagger
     *
     * @see OpenNLP#posTagging(String[], langOptions)
     */
    private final POSTaggerME taggerFr;
    private final POSTaggerME taggerEn;

    /**
     * Private constructor to allow only one {@link OpenNLP} for each Thread
     *
     * @throws IllegalStateException if an error occurred from {@link LoaderNLP} or {@link PropertiesManager}
     */
    private OpenNLP() {
        try {
                //FRENCH
                detectorFr = new SentenceDetectorME(LoaderNLP.getFrSentenceModel());
                tokenizerFr = new TokenizerME(LoaderNLP.getFrTokenizerModel());
                taggerFr = new POSTaggerME(LoaderNLP.getFrPosModel());
                nameFinderOrgFr = new NameFinderME(LoaderNLP.getFrTokenNameFinderModelOrg());
                nameFinderLocFr = new NameFinderME(LoaderNLP.getFrTokenNameFinderModelLoc());
                nameFinderPersFr = new NameFinderME(LoaderNLP.getFrTokenNameFinderModelPers());
                InputStream inputStreamFr = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.dictionaries.path"));
                lemmatizerFr = new SimpleLemmatizer(inputStreamFr);
                inputStreamFr.close();
                //ENGLISH
                detectorEn = new SentenceDetectorME(LoaderNLP.getEnSentenceModel());
                tokenizerEn = new TokenizerME(LoaderNLP.getEnTokenizerModel());
                taggerEn = new POSTaggerME(LoaderNLP.getEnPosModel());
                nameFinderOrgEn = new NameFinderME(LoaderNLP.getEnTokenNameFinderModelOrg());
                nameFinderLocEn = new NameFinderME(LoaderNLP.getEnTokenNameFinderModelLoc());
                nameFinderPersEn = new NameFinderME(LoaderNLP.getEnTokenNameFinderModelPers());
                InputStream inputStreamEn = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.dictionaries.path"));
                lemmatizerEn = new SimpleLemmatizer(inputStreamEn);
                inputStreamEn.close();

        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get instance of {@link OpenNLP} for each thread because Apache OpenNLP is not thread safe
     *
     * @param thread Thread needs {@link OpenNLP}
     * @return Instance of {@link OpenNLP}
     * @throws NullPointerException if thread is null
     * @see OpenNLP#INSTANCES
     */
    public static OpenNLP getOpenNLP(Thread thread) {
        Objects.requireNonNull(thread);
        return INSTANCES.computeIfAbsent(thread, t -> new OpenNLP());
    }

    /**
     * Enum the ner options
     */
    public enum nerOptions {
        LOCATION, PERSON, ORGANIZATION
    }

    /**
     * Enum availables languages
     */
    public enum langOptions {
        FRENCH, ENGLISH, DEFAULT
    }

    /**
     * OpenNLP : split a text in sentences
     *
     * @param text to analyze
     * @return an array of sentences
     * @throws NullPointerException if text is null
     * @see OpenNLP#detectorFr
     * @see OpenNLP#detectorEn
     */
    private String[] detectSentences(String text, langOptions lang) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH: return detectorFr.sentDetect(text);
            case ENGLISH: return detectorEn.sentDetect(text);
            default: return detectorEn.sentDetect(text);
        }
    }

    /**
     * OpenNLP : learnableTokenize. The function tokenize a text
     *
     * @param text to tokenize
     * @return an array of words
     * @throws NullPointerException if text is null
     * @see OpenNLP#tokenizerFr
     * @see OpenNLP#tokenizerEn
     */
    private String[] learnableTokenize(String text, langOptions lang) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH: return tokenizerFr.tokenize(text);
            case ENGLISH: return tokenizerEn.tokenize(text);
            default:return tokenizerEn.tokenize(text);
        }
    }

    /**
     * OpenNLP : posTagging affect a tag to each word (V, NC, NP, ADJ...)
     *
     * @param tokens is a tokenize text
     * @return an array of posTag
     * @throws NullPointerException if tokens is null
     * @see OpenNLP#taggerFr
     * @see OpenNLP#taggerEn
     */
    private String[] posTagging(String[] tokens, langOptions lang) {
        Objects.requireNonNull(tokens);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH:   return taggerFr.tag(tokens);
            case ENGLISH:   return taggerEn.tag(tokens);
            default: return taggerEn.tag(tokens);
        }
    }

    /**
     * OpenNLP : name entity recognizer function. Detect organizations names.
     *
     * @param tokens are an array of string to analyze
     * @return an array of entity detected as an organization
     * @throws NullPointerException if tokens is null
     * @see OpenNLP#nameFinderOrgFr
     * @see OpenNLP#nameFinderOrgEn
     */
    private Span[] findOrganizationName(String[] tokens, langOptions lang) {
        Objects.requireNonNull(tokens);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH:    return nameFinderOrgFr.find(tokens);
            case ENGLISH:   return nameFinderOrgEn.find(tokens);
            default: return nameFinderOrgEn.find(tokens);
        }

    }

    /**
     * OpenNLP : name entity recognizer function. Detect locations names.
     *
     * @param tokens are an array of string to analyze
     * @return an array of entity detected as a location
     * @throws NullPointerException if tokens is null
     * @see OpenNLP#nameFinderLocFr
     * @see OpenNLP#nameFinderLocEn
     */
    private Span[] findLocationName(String[] tokens, langOptions lang) {
        Objects.requireNonNull(tokens);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH:     return nameFinderLocFr.find(tokens);
            case ENGLISH:    return nameFinderLocEn.find(tokens);
            default:  return nameFinderLocEn.find(tokens);
        }
    }

    /**
     * OpenNLP : name entity recognizer function. Detect persons names.
     *
     * @param tokens are an array of string to analyze
     * @return an array of entity detected as a personnality
     * @throws NullPointerException if tokens is null
     * @see OpenNLP#nameFinderPersFr
     * @see OpenNLP#nameFinderPersEn
     */
    private Span[] findPersonName(String[] tokens,langOptions lang) {
        Objects.requireNonNull(tokens);
        Objects.requireNonNull(lang);
        switch (lang)  {
            case FRENCH:     return nameFinderPersFr.find(tokens);
            case ENGLISH:   return nameFinderPersEn.find(tokens);
            default:        return nameFinderPersEn.find(tokens);
        }
    }

    /**
     * OpenNLP : lemmatization. The function simplify the step of POStagging for the verbs category.
     *
     * @param text to lemmatize
     * @return Map of each lemmatize word with the POStag associate
     * @throws NullPointerException if text is null
     * @see OpenNLP#lemmatizerFr
     * @see OpenNLP#lemmatizerEn
     */
    private Map<String, String> lemmatize(String text, langOptions lang) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(lang);

        DictionaryLemmatizer lemmatizer;
        switch(lang){
            case FRENCH: lemmatizer = this.lemmatizerFr;
            case ENGLISH: lemmatizer = this.lemmatizerEn;
            default: lemmatizer = this.lemmatizerEn;
        }
        Map<String, String> lemmatizedTokens = new HashMap<>();
        // Split tweet text content in sentences
        String[] sentences = detectSentences(text, lang);
        // For each sentence, tokenize and tag before lemmatizing
        for (String sentence : sentences) {
            // Split each sentence in tokens
            String[] learnableTokens = learnableTokenize(sentence, lang);
            // Get tag for each token
            String[] tags = posTagging(learnableTokens, lang);
            // Get lemmatize form of each token
            for (int i = 0; i < learnableTokens.length; i++) {
                if (tags[i].startsWith("V") && tags[i].length() > 1) {
                    //if the POStag start with V, we just keep the tag V for simplify the lemmatization with the dictionnary
                    tags[i] = "V";
                }
                lemmatizedTokens.put(lemmatizer.lemmatize(learnableTokens[i], tags[i]), tags[i]);
            }
        }
        return lemmatizedTokens;
    }

    /**
     * Apply the OpenNLP Lemmatization with a dictionnary. Keep only words with the verbs and nouns.
     *
     * @param post  is the text to lemmatize
     * @param limit is the limit to have the n first characters
     * @return list of selected words.
     * @throws NullPointerException if post is null
     */
    public List<String> applyNLPlemma(String post, langOptions lang, int limit) {
        Objects.requireNonNull(post);
        String tmpPost = post;
        if (tmpPost.length() > limit) {
            tmpPost = post.substring(0, limit);
        }
        Map<String, String> input;
        List<String> output = new ArrayList<>();
        input = lemmatize(tmpPost, lang);
        input.forEach((w, pos) -> {
            if (w.startsWith("#")) {
                output.add(w);
            } else {
                if (pos.startsWith("N") || pos.startsWith("V")) {
                    output.add(w);
                }
            }
        });
        return output;
    }

    /**
     * Apply the OpenNLP Lemmatization with a dictionnary. Keep only words with the verbs and nouns.
     *
     * @param post is the text to lemmatize. We only use the 1250 first characters
     * @return list of selected words.
     * @throws NullPointerException if post is null
     */
    public List<String> applyNLPlemma(String post, langOptions lang) {
        Objects.requireNonNull(post);
        return applyNLPlemma(post, lang, 1250);
    }

    /**
     * Apply the ÖpenNLP ner (name entity recognizer) algorithm on a text. Keep only distinct words from a text.
     *
     * @param post to analyze
     * @param ner  ENUM : LOCATION, ORGANIZATION or PERSON : type of NER analyse
     * @return List of selected words by NER
     * @throws NullPointerException if post or ner is null
     */
    public List<String> applyNLPner(String post, nerOptions ner, langOptions lang) {
        Objects.requireNonNull(post);
        Objects.requireNonNull(ner);
        List<String> words = new ArrayList<>();
        Span[] spans;
        String[] sentences = detectSentences(post, lang);
        for (String sentence : sentences) {
            String[] learnableTokens = learnableTokenize(sentence, lang);
            switch (ner.toString()) {
                case "LOCATION":
                    spans = findLocationName(learnableTokens, lang);
                    break;
                case "ORGANIZATION":
                    spans = findOrganizationName(learnableTokens, lang);
                    break;
                case "PERSON":
                    spans = findPersonName(learnableTokens, lang);
                    break;
                default:
                    LOGGER.warn("Bad NER option.\n use : 'LOCATION', 'PERSON' or 'ORGANIZATION'");
                    return words; //return empty list
            }
            Arrays.asList(Span.spansToStrings(spans, learnableTokens)).forEach(words::add);
        }
        return words;
    }
}