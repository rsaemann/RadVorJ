/*
 * The MIT License
 *
 * Copyright 2019 saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package rain;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.GregorianCalendar;

/**
 *
 * @author saemann
 */
public class RadolanData {

    public static String product;

    /**
     * Values as integer stored in the file. beginning with lower-left corner.
     * first index i: rows (horizontal) 0=South ; secondindex j: column 0=West
     */
    public static int[][] values;
    /**
     * number of horizontal elements
     */
    public static int x;
    /**
     * Number of vertical elements
     */
    public static int y;
    /**
     * Lead time in minutes
     */
    public static int leadTime;
    
    /**
     * Time of creation (not time of forecast)
     */
    public static GregorianCalendar productionTime;

    private double lowerleftLat, lowerleftLon, upperleftLat, upperleftLon, lowerRightLat, lowerRightLon, upperRightLat, upperRightLon;

    public RadolanData(String product, int[][] values, int x, int y, int leadTime, GregorianCalendar productionTime, double lowerleftLat, double lowerleftLon, double upperleftLat, double upperleftLon, double lowerRightLat, double lowerRightLon, double upperRightLat, double upperRightLon) {
        this.product = product;
        this.values = values;
        this.x = x;
        this.y = y;
        this.leadTime = leadTime;
        this.productionTime = productionTime;
        this.lowerleftLat = lowerleftLat;
        this.lowerleftLon = lowerleftLon;
        this.upperleftLat = upperleftLat;
        this.upperleftLon = upperleftLon;
        this.lowerRightLat = lowerRightLat;
        this.lowerRightLon = lowerRightLon;
        this.upperRightLat = upperRightLat;
        this.upperRightLon = upperRightLon;
    }

    public String toHTMLString() {
        return "<html>" + product + "<br> " + productionTime.getTime().toGMTString() + "<br>(" + productionTime.getTime().toLocaleString() + " local)<br>" + x + " x " + y + "<br>" + leadTime + "min lead</html>";
    }

    public BufferedImage createImage() {
        if (this.values == null) {
            throw new NullPointerException("No data values read.");
        }
        BufferedImage bi = new BufferedImage(this.values.length, this.values[0].length, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = bi.getRaster();
        for (int iy = 0; iy < values.length; iy++) {
            int yt = y - 1 - iy;//inverse Y for top down orientation
            for (int xt = 0; xt < values[0].length; xt++) {
                int value = values[iy][xt];
                if (value == Integer.MIN_VALUE) {
                    raster.setPixel(xt, yt, new int[]{100, 80, 80});
                } else if (value == 0) {
                    raster.setPixel(xt, yt, new int[]{0, 0, 0});
                } else {
                    raster.setPixel(xt, yt, new int[]{(int) Math.min(255, Math.max(0, value > 0 ? 100 : 0) + value * 10), value > 0 ? 200 : 0, 0/*(int) Math.min(255, Math.max(0, (value*10)) )*/});
                }
            }
//            System.out.println(yt);
        }
        bi.setData(raster);

        return bi;
    }

    /**
     *
     * @param i row (y)
     * @param j column (x)
     * @return
     */
    public double[] getLatLonForDataIndex(double i, double j) {
        double fracX = j / (double) (this.x);
        double fracY = i / (double) (this.y);

        double upperLat = upperleftLat + (upperRightLat - upperleftLat) * fracX;
        double upperLon = upperleftLon + (upperRightLon - upperleftLon) * fracX;

        double lowerLat = lowerleftLat + (lowerRightLat - lowerleftLat) * fracX;
        double lowerLon = lowerleftLon + (lowerRightLon - lowerleftLon) * fracX;

        double lat = lowerLat + (upperLat - lowerLat) * fracY;
        double lon = lowerLon + (upperLon - lowerLon) * fracY;

        return new double[]{lat, lon};
    }

    public double[] getLatLonForPositionIndex(double x, double y) {
        double fracX = x / (double) (this.x);
        double fracY = y / (double) (this.y);

        double upperLat = upperleftLat + (upperRightLat - upperleftLat) * fracX;
        double upperLon = upperleftLon + (upperRightLon - upperleftLon) * fracX;

        double lowerLat = lowerleftLat + (lowerRightLat - lowerleftLat) * fracX;
        double lowerLon = lowerleftLon + (lowerRightLon - lowerleftLon) * fracX;

        double lat = lowerLat + (upperLat - lowerLat) * fracY;
        double lon = lowerLon + (upperLon - lowerLon) * fracY;

        return new double[]{lat, lon};
    }

    /**
     *
     * @param lat
     * @param lon
     * @return (x,y) indizes
     */
    public double[] getPositionIndicesForLatLon(double lat, double lon) {
        if (x == 900 && y == 900) {
            double diffLat = lat - 51;
            double difflon = lon - 9;
            System.out.println("diff: x=" + difflon + "  y:" + diffLat);
            double y;
            if (Math.abs(diffLat) < 0.000001) {
                y = 450;
            } else {
                y = 450 + 111 * diffLat; //~111km/1°N everywhere
            }
            double x;
            if (Math.abs(difflon) < 0.00000001) {
                x = 450;
            } else {
                x = 450 + 68 * difflon;  //~68km/1°E in Germany
            }

            return new double[]{x, this.y - y};
        }
        throw new UnsupportedOperationException("Not yet implemented");

//        return new double[]{lat,lon};
    }

    /**
     *
     * @param lat
     * @param lon
     * @return (i,j) (row, column) in data
     */
    public double[] getDataIndicesForLatLon(double lat, double lon) {
        double[] xy = getPositionIndicesForLatLon(lat, lon);
        return new double[]{y - xy[1], xy[0]};
    }

}
