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

public class LanguageDetection {
    /**
     * Logger used to log all information in this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNLP.class);
    private final List<LanguageProfile> languageProfiles;
    private final LanguageDetector languageDetector;
    private final TextObjectFactory textObjectFactory;

    public LanguageDetection() {
        try {
            //load all languages:
            this.languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            //build language detector:
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
            //create a text object factory
            textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }


    public LdLocale detectLanguage(String text) {
        TextObject textObject = this.textObjectFactory.forText(text);
        com.google.common.base.Optional<LdLocale> lang = this.languageDetector.detect(textObject);
        if (lang.isPresent()) {
            return lang.get();
        }
        return null;
    }

/*
    public static void main(String[] args) {
        String text = "Bonjour, je m'appelle Sandy.";
        LanguageDetection languageDetection = new LanguageDetection();
        LdLocale lang = languageDetection.detectLanguage(text);
        if (lang != null)
            lang.getLanguage();
        System.out.println(lang);
    }
    */
}
