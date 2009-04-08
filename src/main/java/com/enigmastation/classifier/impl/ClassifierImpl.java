package com.enigmastation.classifier.impl;

import com.enigmastation.classifier.*;
import com.enigmastation.extractors.WordLister;
import com.enigmastation.extractors.impl.StemmingWordLister;
import javolution.util.FastSet;

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * This is a simple Bayesian calculation class. It was ported from Python contained in the book
 * "<a href="http://www.oreilly.com/catalog/9780596529321/index.html">Programming Collective Intelligence</a>,"
 * by Toby Segaran.
 *
 * @author <a href="mailto:joeo@enigmastation.com">Joseph B. Ottinger</a>
 * @version $Revision: 36 $
 */
public class ClassifierImpl implements Classifier {
    /**
     * In Segaran's book, this is referred to as "fc"
     */
    private FeatureMap categoryFeatureMap = createFeatureMap();

    public FeatureMap createFeatureMap() {
        return new FeatureMap();
    }

    /**
     * In Segaran's book, this is referred to as "cc"
     */
    private ClassifierMap categoryDocCount = createClassifierMap();

    public ClassifierMap createClassifierMap() {
        return new ClassifierMap();
    }

    protected WordLister extractor = null;
    private Set<ClassifierListener> listeners = new FastSet<ClassifierListener>();

    public void addListener(ClassifierListener listener) {
        listeners.add(listener);
    }

    /**
     * Convenience method for loaders.
     *
     * @deprecated since 1.0.5
     */
    public void addCategory(String category) {
        //throw new NoSuchMethodError("Please don't use ClassifierImpl.addCategory() explicitly any more.");
    }

    public ClassifierImpl(WordLister w) {
        extractor = w;
    }

    public ClassifierImpl() {
        WordLister wl = null;
        try {
            WordListerLocator locator = (WordListerLocator) Class.forName("com.enigmastation.classifier.impl.ServiceLoaderWordListerLocatorImpl")
                    .newInstance();
            wl = locator.locate();
        } catch (NoClassDefFoundError e) {
            // this is thrown when the VM can find ServiceLoaderWordListerLocatorImpl but not
            // java.util.ServiceLoader class
        } catch (ClassNotFoundException e) {
            // this in theory shouldn't happen as ServiceLoaderWordListerLocatorImpl does exist
        } catch (IllegalAccessException e) {
            // could be that a security manager has caused this
        } catch (InstantiationException e) {
            // just unable to create the object rethrow 
            if (e.getCause() instanceof RuntimeException)
                throw (RuntimeException) e.getCause();
            throw new RuntimeException(e.getCause());
        }

        if (wl == null) {
            wl = new StemmingWordLister();
        }
        extractor = wl;
    }

    /**
     * Increase the count of a feature/category pair.
     * <p/>
     * Direct port from Segaran's book, including method name
     *
     * @param feature  the feature (the 'word')
     * @param category the category
     */
    void incf(String feature, String category) {
        ClassifierMap fm = getCategoryFeatureMap().getFeature(feature);
        fm.incrementCategory(category);

        FeatureIncrement fi = null;
        for (ClassifierListener l : listeners) {
            if (fi == null) {
                fi = new FeatureIncrement(feature, category, fm.get(category));
            }
            l.handleFeatureUpdate(fi);
        }

    }

    /**
     * Increase the count of a category.
     * Direct port from Segaran's book, including method name
     *
     * @param category the category to increment
     */
    void incc(String category) {
        getCategoryDocCount().incrementCategory(category);

        CategoryIncrement ci = null;
        for (ClassifierListener l : listeners) {
            if (ci == null) {
                ci = new CategoryIncrement(category, getCategoryDocCount().get(category));
                ci.setCountDelta(1);
            }
            l.handleCategoryUpdate(ci);
        }

    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @param feature  the feature
     * @param category the category to query
     * @return the number of times a feature has appeared in a category
     */
    double fcount(String feature, String category) {
        if (getCategoryFeatureMap().containsKey(feature) && getCategoryFeatureMap().get(feature).containsKey(category)) {
            return getCategoryFeatureMap().get(feature).get(category);
        }
        return 0.0;
    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @param category the category to count items for
     * @return the number of items in a category
     */
    double catcount(String category) {
        return getCategoryDocCount().get(category);
    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @return the total number of items
     */
    double totalcount() {
        return getCategoryDocCount().getTotalCount();
    }

    double totalcount(String feature) {
        if (getCategoryFeatureMap().containsKey(feature)) {
            return getCategoryFeatureMap().get(feature).getTotalCount();
        }
        return 0.0;
    }

    /**
     * Direct port from Segaran's book, including method name.
     *
     * @return the list of all getCategories
     */
    public final Set<String> getCategories() {
        return Collections.unmodifiableSet(getCategoryDocCount().keySet());
    }

    public void train(Object item, String category) {
        Set<String> features = extractor.getUniqueWords(item);

        for (String f : features) {
            incf(f, category);
        }
        incc(category);
    }

    /**
     * Convenience method for descendant classes - aids in porting from Segaran's book.
     * <p/>
     * I want to change this method to use the arithmetic exception *only* if it's rare. It's possible
     * that determining rarity might be even more expensive, though.
     *
     * @param feature  the feature to consider
     * @param category the category
     * @return the feature probability for the class
     */
    protected double fprob(String feature, String category) {
        try {
            return fcount(feature, category) / catcount(category);
        } catch (ArithmeticException ae) {
            return 0;
        }
    }

    /**
     * @param feature  the feature to consider
     * @param category the category
     * @return the feature probability for the class
     */
    public final double getFeatureProbability(String feature, String category) {
        return fprob(feature, category);
    }


    private double WEIGHT = 1.0;
    private double ASSUMED_PROBABILITY = 0.5;

    protected double weightedprob(String feature, String category) {
        return getWeightedProbability(feature, category);
    }

    /**
     * @param feature  The feature to consider
     * @param category the category to consider weight for
     * @return the weighted probability
     */
    public double getWeightedProbability(String feature, String category) {
        double basicprob = getFeatureProbability(feature, category);
        double totals = totalcount(feature);
        return ((WEIGHT * ASSUMED_PROBABILITY) + (totals * basicprob)) / (WEIGHT + totals);
    }

    public final FeatureMap getCategoryFeatureMap() {
        return categoryFeatureMap;
    }

    public final ClassifierMap getCategoryDocCount() {
        return categoryDocCount;
    }
}
