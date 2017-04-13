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
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Load NLP binaries to permit to library OpenNLP to be executed in multiple thread
 *
 * @author ikb4stream
 * @version 1.0
 */
class FRLoaderNLP {
    /**
     * Properties of this class
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(FRLoaderNLP.class);
    /**
     * Logger used to log all information in this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FRLoaderNLP.class);
    /**
     * Load model to apply person name finder
     *
     * @see FRLoaderNLP#getTokenNameFinderModelPers()
     */
    private static final TokenNameFinderModel TOKEN_NAME_FINDER_MODEL_PERS;
    /**
     * Load model to apply organization name finder
     *
     * @see FRLoaderNLP#getTokenNameFinderModelOrg()
     */
    private static final TokenNameFinderModel TOKEN_NAME_FINDER_MODEL_ORG;
    /**
     * Load model to apply localisation name finder
     *
     * @see FRLoaderNLP#getTokenNameFinderModelLoc()
     */
    private static final TokenNameFinderModel TOKEN_NAME_FINDER_MODEL_LOC;
    /**
     * Load model to apply tokenization
     *
     * @see FRLoaderNLP#getTokenizerModel()
     */
    private static final TokenizerModel TOKENIZER_MODEL;
    /**
     * Load model to apply sentence detection
     *
     * @see FRLoaderNLP#getSentenceModel()
     */
    private static final SentenceModel SENTENCE_MODEL;
    /**
     * Load model to apply part-of-speech tagger
     *
     * @see FRLoaderNLP#getPosModel()
     */
    private static final POSModel POS_MODEL;

    /**
     * Private constructor to block instantiation
     * This class provides only static method
     */
    private FRLoaderNLP() {

    }

    static {
        try {
            InputStream fileFrSentBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.sentence"));
            SENTENCE_MODEL = new SentenceModel(fileFrSentBin);
            fileFrSentBin.close();
            InputStream fileFrTokenBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenizer"));
            TOKENIZER_MODEL = new TokenizerModel(fileFrTokenBin);
            fileFrTokenBin.close();
            InputStream fileFrPosMaxent2Bin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.posmodel"));
            POS_MODEL = new POSModel(fileFrPosMaxent2Bin);
            fileFrPosMaxent2Bin.close();
            InputStream frNerOrganizationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.organization"));
            TOKEN_NAME_FINDER_MODEL_ORG = new TokenNameFinderModel(frNerOrganizationBin);
            frNerOrganizationBin.close();
            InputStream fileFrNerLocationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.location"));
            TOKEN_NAME_FINDER_MODEL_LOC = new TokenNameFinderModel(fileFrNerLocationBin);
            fileFrNerLocationBin.close();
            InputStream fileNerPersonBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.person"));
            TOKEN_NAME_FINDER_MODEL_PERS = new TokenNameFinderModel(fileNerPersonBin);
            fileNerPersonBin.close();
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get model to apply person name finder
     *
     * @return {@link FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_PERS}
     * @see FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_PERS
     */
    static TokenNameFinderModel getTokenNameFinderModelPers() {
        return TOKEN_NAME_FINDER_MODEL_PERS;
    }

    /**
     * Get model to apply organization name finder
     *
     * @return {@link FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_ORG}
     * @see FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_ORG
     */
    static TokenNameFinderModel getTokenNameFinderModelOrg() {
        return TOKEN_NAME_FINDER_MODEL_ORG;
    }

    /**
     * Get model to apply location name finder
     *
     * @return {@link FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_LOC}
     * @see FRLoaderNLP#TOKEN_NAME_FINDER_MODEL_LOC
     */
    static TokenNameFinderModel getTokenNameFinderModelLoc() {
        return TOKEN_NAME_FINDER_MODEL_LOC;
    }

    /**
     * Get model to apply tokenization
     *
     * @return {@link FRLoaderNLP#TOKENIZER_MODEL}
     * @see FRLoaderNLP#TOKENIZER_MODEL
     */
    static TokenizerModel getTokenizerModel() {
        return TOKENIZER_MODEL;
    }

    /**
     * Get model to apply sentence detection
     *
     * @return {@link FRLoaderNLP#SENTENCE_MODEL}
     * @see FRLoaderNLP#SENTENCE_MODEL
     */
    static SentenceModel getSentenceModel() {
        return SENTENCE_MODEL;
    }

    /**
     * Get model to apply part-of-speech tagger
     *
     * @return {@link FRLoaderNLP#POS_MODEL}
     * @see FRLoaderNLP#POS_MODEL
     */
    static POSModel getPosModel() {
        return POS_MODEL;
    }
}
