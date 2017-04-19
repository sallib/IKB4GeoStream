package com.waves_rsp.ikb4stream.core.util;


import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.waves_rsp.ikb4stream.core.util.nlp.OpenNLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * https://github.com/optimaize/language-detector
 */
public class LanguageDetection {
    /**
     * Logger used to log all information in this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageDetection.class);
    private final LanguageDetector languageDetector;
    private final TextObjectFactory textObjectFactory;

    public LanguageDetection() {
        try {
            //load all languages:
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            //build language detector:
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
            //create a text object factory
            textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
        } catch (IOException e) {
            LOGGER.error("Language detection failed " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param text
     * @return fr = french
     * en = english
     * ar = arabic
     * es = spanish
     */
    public OpenNLP.langOptions detectLanguage(String text) {
        TextObject textObject = this.textObjectFactory.forText(text);
        com.google.common.base.Optional<LdLocale> lang = this.languageDetector.detect(textObject);
        if (lang.isPresent()) {
            if (lang.get().getLanguage().equals("fr")) {
                return OpenNLP.langOptions.FRENCH;
            } else if (lang.get().getLanguage().equals("en")) {
                return OpenNLP.langOptions.ENGLISH;
            } else {
                return OpenNLP.langOptions.DEFAULT;
            }
        }
        //not recognize the language
        return OpenNLP.langOptions.DEFAULT;
    }

}
