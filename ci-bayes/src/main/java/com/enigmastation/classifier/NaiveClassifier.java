package com.enigmastation.classifier;

/**
 * This is the Naive Bayesian interface. Ideally, one would use the only the
 * getClassification() method to return a classification from the engine.
 * <p/>
 * Like everything else in this package, it was ported from Python contained in the book
 * "<a href="http://www.oreilly.com/catalog/9780596529321/index.html">Programming Collective Intelligence</a>,"
 * by Toby Segaran.
 *
 * @author <a href="mailto:joeo@enigmastation.com">Joseph B. Ottinger</a>
 * @version $Revision: 38 $
 * @see com.enigmastation.classifier.impl.NaiveClassifierImpl
 */
public interface NaiveClassifier extends Classifier {
    double getDocumentProbabilityForCategory(Object item, String category);

    double getProbabilityForCategory(Object item, String category);

    String getClassification(Object item, String defaultCat);

    @SuppressWarnings({"SameParameterValue"})
    void setCategoryThreshold(String cat, double t);

    double getCategoryThreshold(String cat);

    ClassifierProbability[] getProbabilities(final Object item);

    void normalizeProbabilities(ClassifierProbability[] probabilities);

    String getClassification(Object o);

    void addListener(ClassificationListener listener);
}
