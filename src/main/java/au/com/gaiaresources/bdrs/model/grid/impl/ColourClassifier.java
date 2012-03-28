package au.com.gaiaresources.bdrs.model.grid.impl;

import java.awt.*;
import java.util.Arrays;

class ColourClassifier {
    private Color baseColour;
    private int startOpacity;
    private int endOpacity;

    private Color[] colours;
    private int[] values;
    private double step;

    public ColourClassifier(int[] values, int levels, Color baseColour,
                            int startOpacity, int endOpacity) {
        this.colours = new Color[levels];
        this.values = values;
        Arrays.sort(this.values);

        this.baseColour = baseColour;
        this.startOpacity = startOpacity;
        this.endOpacity = endOpacity;

        this.step = calculateStep();

        initialiseColours();
    }

    private double calculateStep() {
        int minValue = values[0];
        int maxValue = values[values.length - 1];

        return ((double) (maxValue - minValue)) / (double) colours.length;
    }

    private void initialiseColours() {
        int opacityStep = (endOpacity - startOpacity) / colours.length;
        for (int o = startOpacity, i = 0; o < endOpacity; o += opacityStep, i++) {
            colours[i] = new Color(baseColour.getRed(), baseColour
                    .getGreen(), baseColour.getBlue(), startOpacity
                    + (i * opacityStep));
        }
    }

    public Color getColour(int value) {
        int colourIndex = (int) Math.round((value - values[0]) / step);
        return colours[Math.max(0, colourIndex - 1)];
    }

    public Color[] getColours() {
        return Arrays.copyOf(this.colours, this.colours.length);
    }
}
