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
class LoaderNLP {

    /**
     * Properties of this class
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class)
     */
    static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(LoaderNLP.class);
    /**
     * Logger used to log all information in this class
     */
    static final Logger LOGGER = LoggerFactory.getLogger(LoaderNLP.class);

    /**
     * Load model to apply person name finder
     *
     * @see LoaderNLP#getFrTokenNameFinderModelPers()
     */
    static final TokenNameFinderModel FR_TOKEN_NAME_FINDER_MODEL_PERS;
    static final TokenNameFinderModel EN_TOKEN_NAME_FINDER_MODEL_PERS;
    /**
     * Load model to apply organization name finder
     *
     * @see LoaderNLP#getFrTokenNameFinderModelOrg()
     */
    static final TokenNameFinderModel FR_TOKEN_NAME_FINDER_MODEL_ORG;
    static final TokenNameFinderModel EN_TOKEN_NAME_FINDER_MODEL_ORG;
    /**
     * Load model to apply localisation name finder
     *
     * @see LoaderNLP#getFrTokenNameFinderModelLoc()
     */
    static final TokenNameFinderModel FR_TOKEN_NAME_FINDER_MODEL_LOC;
    static final TokenNameFinderModel EN_TOKEN_NAME_FINDER_MODEL_LOC;
    /**
     * Load model to apply tokenization
     *
     * @see LoaderNLP#getFrTokenizerModel()
     */
    static final TokenizerModel FR_TOKENIZER_MODEL;
    static final TokenizerModel EN_TOKENIZER_MODEL;
    /**
     * Load model to apply sentence detection
     *
     * @see LoaderNLP#getFrSentenceModel()
     */
    static final SentenceModel FR_SENTENCE_MODEL;
    static final SentenceModel EN_SENTENCE_MODEL;
    /**
     * Load model to apply part-of-speech tagger
     *
     * @see LoaderNLP#getFrPosModel()
     */
    static final POSModel FR_POS_MODEL;
    static final POSModel EN_POS_MODEL;


    /**
     * Private constructor to block instantiation
     * This class provides only static method
     */
    private LoaderNLP() {

    }

    /**
     * Get model to apply person name finder
     *
     * @return {@link LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_PERS}
     * @see LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_PERS
     */
    static TokenNameFinderModel getFrTokenNameFinderModelPers() {
        return FR_TOKEN_NAME_FINDER_MODEL_PERS;
    }
    static TokenNameFinderModel getEnTokenNameFinderModelPers() {return EN_TOKEN_NAME_FINDER_MODEL_PERS;}

    /**
     * Get model to apply organization name finder
     *
     * @return {@link LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_ORG}
     * @see LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_ORG
     */
    static TokenNameFinderModel getFrTokenNameFinderModelOrg() {
        return FR_TOKEN_NAME_FINDER_MODEL_ORG;
    }
    static TokenNameFinderModel getEnTokenNameFinderModelOrg() {return EN_TOKEN_NAME_FINDER_MODEL_ORG;}

    /**
     * Get model to apply location name finder
     *
     * @return {@link LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_LOC}
     * @see LoaderNLP#FR_TOKEN_NAME_FINDER_MODEL_LOC
     */
    static TokenNameFinderModel getFrTokenNameFinderModelLoc() {
        return FR_TOKEN_NAME_FINDER_MODEL_LOC;
    }
    static TokenNameFinderModel getEnTokenNameFinderModelLoc() {
        return EN_TOKEN_NAME_FINDER_MODEL_LOC;
    }
    /**
     * Get model to apply tokenization
     *
     * @return {@link LoaderNLP#FR_TOKENIZER_MODEL}
     * @see LoaderNLP#FR_TOKENIZER_MODEL
     */
    static TokenizerModel getFrTokenizerModel() {
        return FR_TOKENIZER_MODEL;
    }
    static TokenizerModel getEnTokenizerModel() {
        return EN_TOKENIZER_MODEL;
    }

    /**
     * Get model to apply sentence detection
     *
     * @return {@link LoaderNLP#FR_SENTENCE_MODEL}
     * @see LoaderNLP#FR_SENTENCE_MODEL
     */
    static SentenceModel getFrSentenceModel() {
        return FR_SENTENCE_MODEL;
    }
    static SentenceModel getEnSentenceModel() {
        return EN_SENTENCE_MODEL;
    }
    /**
     * Get model to apply part-of-speech tagger
     *
     * @return {@link LoaderNLP#FR_POS_MODEL}
     * @see LoaderNLP#FR_POS_MODEL
     */
    static POSModel getFrPosModel() {return FR_POS_MODEL;}
    static POSModel getEnPosModel() {
        return EN_POS_MODEL;
    }

    static {
        try {

            //FRENCH
            InputStream fileFrSentBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.sentence"));
            FR_SENTENCE_MODEL = new SentenceModel(fileFrSentBin);
            fileFrSentBin.close();
            InputStream fileFrTokenBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenizer"));
            FR_TOKENIZER_MODEL = new TokenizerModel(fileFrTokenBin);
            fileFrTokenBin.close();
            InputStream fileFrPosMaxent2Bin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.posmodel"));
            FR_POS_MODEL = new POSModel(fileFrPosMaxent2Bin);
            fileFrPosMaxent2Bin.close();
            InputStream frNerOrganizationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.organization"));
            FR_TOKEN_NAME_FINDER_MODEL_ORG = new TokenNameFinderModel(frNerOrganizationBin);
            frNerOrganizationBin.close();
            InputStream fileFrNerLocationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.location"));
            FR_TOKEN_NAME_FINDER_MODEL_LOC = new TokenNameFinderModel(fileFrNerLocationBin);
            fileFrNerLocationBin.close();
            InputStream fileFrNerPersonBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.fr.tokenname.person"));
            FR_TOKEN_NAME_FINDER_MODEL_PERS = new TokenNameFinderModel(fileFrNerPersonBin);
            fileFrNerPersonBin.close();


            //ENGLISH
            InputStream fileEnSentBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.sentence"));
            EN_SENTENCE_MODEL = new SentenceModel(fileEnSentBin);
            fileEnSentBin.close();
            InputStream fileEnTokenBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.tokenizer"));
            EN_TOKENIZER_MODEL = new TokenizerModel(fileEnTokenBin);
            fileEnTokenBin.close();
            InputStream fileEnPosMaxent2Bin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.posmodel"));
            EN_POS_MODEL = new POSModel(fileEnPosMaxent2Bin);
            fileEnPosMaxent2Bin.close();
            InputStream enNerOrganizationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.tokenname.organization"));
            EN_TOKEN_NAME_FINDER_MODEL_ORG = new TokenNameFinderModel(enNerOrganizationBin);
            enNerOrganizationBin.close();
            InputStream fileEnNerLocationBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.tokenname.location"));
            EN_TOKEN_NAME_FINDER_MODEL_LOC = new TokenNameFinderModel(fileEnNerLocationBin);
            fileEnNerLocationBin.close();
            InputStream fileEnNerPersonBin = new FileInputStream(PROPERTIES_MANAGER.getProperty("nlp.en.tokenname.person"));
            EN_TOKEN_NAME_FINDER_MODEL_PERS = new TokenNameFinderModel(fileEnNerPersonBin);
            fileEnNerPersonBin.close();
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }


}
