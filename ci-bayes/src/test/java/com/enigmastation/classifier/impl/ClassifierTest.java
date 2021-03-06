package com.enigmastation.classifier.impl;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.enigmastation.classifier.Classifier;
import com.enigmastation.classifier.ClassifierProbability;
import com.enigmastation.classifier.FisherClassifier;
import com.enigmastation.classifier.NaiveClassifier;
import com.enigmastation.dao.db4o.CategoryDAOImpl;
import com.enigmastation.dao.db4o.FeatureDAOImpl;
import com.enigmastation.extractors.WordLister;
import com.enigmastation.extractors.impl.SimpleWordLister;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ClassifierTest {
    public static void main(String[] args) {
        ClassifierTest c = new ClassifierTest();

        c.testCategoryProbabilities();
        c.testFisherClassifier();
        c.testIncc();
        c.testInternalFeatureCount();
        c.testNaiveCategoryAssignment();
        c.testClassifierProbabilitySort();
        /*
           * try { c.testSerializableSave(); } catch (IOException e) {
           * e.printStackTrace(); //To change body of catch statement use File |
           * Settings | File Templates. } try { c.testSerializableLoad(); } catch
           * (IOException e) { e.printStackTrace(); //To change body of catch
           * statement use File | Settings | File Templates. } catch
           * (ClassNotFoundException e) { e.printStackTrace(); //To change body of
           * catch statement use File | Settings | File Templates. }
           */
        c.testWeightedProbabilityByCategory();
        c.testWords();
    }

    @BeforeTest
    public void clearDB() {
        ObjectContainer db = Db4oEmbedded.openFile(Db4oEmbedded
                .newConfiguration(), "dbFile");
        ObjectSet set = db.queryByExample(new Object());
        for (Object o : set) {
            System.out.println("deleting object " + o + " from database");
            db.delete(o);
        }

        db.close();
    }

    @Test(groups = {"fulltest", "normal"})
    public void testClassifierProbabilitySort() {
        ClassifierProbability c1 = new ClassifierProbability(), c2 = new ClassifierProbability();
        c1.setCategory("foo");
        c2.setCategory("foo");
        assertEquals(c1.compareTo(c2), 0);
        c2.setCategory("bar");
        c1.setScore(0.1);
        c2.setScore(0.2);
        assertEquals(c1.compareTo(c2), 1);
        c2.setScore(0.05);
        assertEquals(c1.compareTo(c2), -1);
    }

    @Test(groups = {"fulltest", "normal"})
    public void testWords() {
        WordLister w = new SimpleWordLister();
        assertEquals(w.getUniqueWords("Now is the time - 'now'").size(), 3);
    }

    @Test(groups = {"fulltest", "normal"})
    public void testIncc() {
        ClassifierImpl impl = new ClassifierImpl();
        impl.categoryDAO = new CategoryDAOImpl();
        impl.setWordLister(new SimpleWordLister());
        impl.init();
        impl.incc("foo");
    }

    @Test(groups = {"fulltest", "normal"})
    public void testInternalFeatureCount() {
        ClassifierImpl impl = new ClassifierImpl();
        impl.setWordLister(new SimpleWordLister());
        impl.init();
        impl.train("the quick brown fox jumps over the lazy dog", "good");
        impl.train("make quick money in the online casino", "bad");
        assertEquals(impl.fcount("quick", "good"), 1.0, 0.1);
        assertEquals(impl.fcount("quick", "bad"), 1.0, 0.1);
    }

    @Test(groups = {"normal"})
    public void testIssue2() {
        FisherClassifierImpl fc = new FisherClassifierImpl();
        fc.init();
        fc.train("The quick brown fox jumps over the lazy dog's tail", "good");
        fc.train("Make money fast!", "bad");
        String classification = fc.getClassification("money");
        assertEquals(classification, "bad");
    }

    @Test(groups = {"fulltest", "normal"})
    public void testWeightedProbabilityByCategory() {
        Classifier cl = getClassifier();
        assertEquals(cl.getFeatureProbability("quick", "good"), 0.666666,
                0.000001);
        assertEquals(cl.getWeightedProbability("money", "good"), 0.25, 0.001);

        sampleTrain(cl);

        assertEquals(cl.getWeightedProbability("money", "good"), 0.166666,
                0.00001);

    }

    @Test(groups = {"fulltest", "normal"})
    public void testCategoryProbabilities() {
        NaiveClassifier nc = getNaiveClassifier();
        assertEquals(nc.getProbabilityForCategory("quick rabbit", "good"),
                0.15624, 0.0001);
        assertEquals(nc.getProbabilityForCategory("quick rabbit", "bad"), 0.05,
                0.01);
    }

    @Test(groups = {"fulltest", "normal"})
    public void testNaiveCategoryAssignment() {
        NaiveClassifier nc = getNaiveClassifier();
        if (!nc.getClassification("quick rabbit", "unknown").equals("good")) {
            throw new RuntimeException("failed getting good");
        }
        if (!nc.getClassification("quick money", "unknown").equals("bad")) {
            throw new RuntimeException("failed getting bad");
        }
        nc.setCategoryThreshold("bad", 3.0);
        if (!nc.getClassification("quick money", "unknown").equals("unknown")) {
            throw new RuntimeException("failed getting unknown");
        }
        for (int i = 0; i < 9; i++) {
            sampleTrain(nc);
        }
        if (!nc.getClassification("quick money", "unknown").equals("bad")) {
            throw new RuntimeException("failed getting bad");
        }
    }

    @Test(groups = {"fulltest", "normal"})
    public void testFisherClassifier() {
        FisherClassifier nc = getFisherClassifier();
        assertEquals(nc.getFeatureProbability("quick", "good"), 0.5714, 0.0001);
        assertEquals(nc.getFeatureProbability("money", "bad"), 1.0, 0.0001);
        assertEquals(nc.getWeightedProbability("money", "bad"), 0.75, 0.001);
        assertEquals(nc.getFisherProbability("quick rabbit", "good"), 0.7801,
                0.0001);
        assertEquals(nc.getFisherProbability("quick rabbit", "bad"), 0.3563,
                0.0001);
        assertEquals(nc.getClassification("quick rabbit", "none"), "good");
        assertEquals(nc.getClassification("quick money", "none"), "bad");
        nc.setMinimum("bad", 0.8);
        assertEquals(nc.getClassification("quick money", "none"), "good");
        nc.setMinimum("good", 0.5);
        assertEquals(nc.getClassification("quick money", "none"), "none");
    }

    Classifier getClassifier() {
        ClassifierImpl cl = new ClassifierImpl();
        cl.categoryDAO = new CategoryDAOImpl();
        cl.featureDAO = new FeatureDAOImpl();
        cl.setWordLister(new SimpleWordLister());
        cl.init();
        sampleTrain(cl);
        return cl;
    }

    NaiveClassifier getNaiveClassifier() {
        NaiveClassifierImpl nc = new NaiveClassifierImpl();
        nc.categoryDAO = new CategoryDAOImpl();
        nc.featureDAO = new FeatureDAOImpl();
        nc.setWordLister(new SimpleWordLister());
        nc.init();
        sampleTrain(nc);
        return nc;
    }

    FisherClassifier getFisherClassifier() {
        FisherClassifierImpl nc = new FisherClassifierImpl();
        nc.categoryDAO = new CategoryDAOImpl();
        nc.featureDAO = new FeatureDAOImpl();
        nc.setWordLister(new SimpleWordLister());
        nc.init();
        sampleTrain(nc);
        return nc;
    }

    private void sampleTrain(Classifier cl) {
        cl.train("Nobody owns the water.", "good");
        cl.train("the quick rabbit jumps fences", "good");
        cl.train("buy pharmaceuticals now", "bad");
        cl.train("make quick money in the online casino", "bad");
        cl.train("the quick brown fox jumps", "good");
    }

    @SuppressWarnings("unused")
    public void testClassificationPerformance() {
        Classifier nc = new FisherClassifierImpl();
        sampleTrain(nc);
        double it = 1000000;
        double prob = nc.getFeatureProbability("quick", "good");
        for (int j = 0; j < 10; j++) {
            double start = System.currentTimeMillis();

            for (int i = 0; i < it; i++) {
                double fprob = nc.getFeatureProbability("quick", "good");
            }

            double stop = System.currentTimeMillis();
            System.out.println((stop - start));
        }
    }
}
