package au.com.gaiaresources.bdrs.model.grid.impl;

import java.util.Arrays;

class EqualClasses {
    int numberClasses;
    double[] breaks;
    double[] collection;

    /**
     * Creates a new instance of EqualClasses
     *
     * @param numberClasses
     * @param fc
     */
    public EqualClasses(int numberClasses, double[] fc) {
        if (numberClasses >= fc.length) {
            int sections = numberClasses * 2;
            double[] newValues = new double[sections];
            double min = fc[0] - 1;
            double max = fc[fc.length - 1] + 1;
            for (int i = 0; i < sections; i++) {
                newValues[i] = (i + 1) * (max - min) / sections;
            }
            setCollection(newValues);
        } else {
            setCollection(fc);
        }
        setNumberClasses(numberClasses);
    }

    /**
     * Getter for property numberClasses.
     *
     * @return Value of property numberClasses.
     */
    public int getNumberClasses() {
        return numberClasses;
    }

    /**
     * Setter for property numberClasses.
     *
     * @param numberClasses New value of property numberClasses.
     */
    public void setNumberClasses(int numberClasses) {
        this.numberClasses = numberClasses;
        if (breaks == null) {
            if (numberClasses > collection.length) {
                breaks = new double[collection.length];
            } else {
                breaks = new double[numberClasses - 1];
            }
        }

        Arrays.sort(collection);

        int step = collection.length / numberClasses;
        int firstCollectionIndex = step;
        if (step == 0) {
            // Case where the number of items in the collection is less than
            // the number of classes (colours).
            step = 1;
        }
        for (int i = firstCollectionIndex, j = 0; j < breaks.length; j++, i += step) {
            breaks[j] = collection[i];
        }
    }

    /**
     * returns the the break points between the classes <b>Note</b> You get
     * one less breaks than number of classes.
     *
     * @return Value of property breaks.
     */
    public double[] getBreaks() {
        return Arrays.copyOf(this.breaks, this.breaks.length);
    }

    /**
     * Setter for property collection.
     *
     * @param collection New value of property collection.
     */
    public void setCollection(double[] collection) {
        this.collection = Arrays.copyOf(collection, this.collection.length);
        ;
    }

}
