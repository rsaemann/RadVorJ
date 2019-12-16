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
package rain.radolan;

import java.util.GregorianCalendar;

/**
 * Information and data of one File from DWD. Radolan, Radvor data storage.
 *
 * @author saemann
 */
public class RadolanData {

    public String product;

    /**
     * Values as integer stored in the file. beginning with lower-left corner.
     * first index i: rows (horizontal) 0=South ; secondindex j: column 0=West
     */
    public int[][] values;
    /**
     * number of horizontal elements
     */
    public int x;
    /**
     * Number of vertical elements
     */
    public int y;
    /**
     * Lead time in minutes
     */
    public int leadTime;

    /**
     * Factor to multiplicate the data values.
     */
    public float factor = 0.1f;// RQ has factor E-1;

    /**
     * Time of creation (not time of forecast)
     */
    public GregorianCalendar productionTime;

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

    public String createTextPicture(boolean reverseY) {
        StringBuffer str = new StringBuffer(x * y * 3);
        for (int i = 0; i < y; i++) {
            int ii = i;
            if (reverseY) {
                ii = y - i - 1;
            }
            for (int j = 0; j < x; j++) {
                int v = values[ii][j];
                if (v == Integer.MIN_VALUE) {
                    str.append(" ").append(",");
                } else {
                    str.append(v).append(",");
                }
            }
            str.append('\n');
        }
        return str.toString();
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
    public double[] getPositionIndicesForLatLon_RS(double lat, double lon) {
        if (x == 900 && y == 900) {
            double diffLat = lat - 51;
            double diffLon = lon - 9;
//            System.out.println("diff: x=" + diffLon + "  y:" + diffLat);
            double y;
            if (Math.abs(diffLat) < 0.000001) {
                y = 450;
            } else {
                y = 450 - 111.1782 * diffLat; //~111km/1°N everywhere
            }
            double x;
            if (Math.abs(diffLon) < 0.00000001) {
                x = 450;
            } else {
                x = 450 + 111.1782 * Math.cos(lat * 0.0174532925) * diffLon;  //~68km/1°E in Germany
            }

            //
//            double m=(1+Math.sin(60/180.*Math.PI))/(1+Math.sin(lat/180.*Math.PI));
//            System.out.println("m("+lat+") = "+m);
//            x=6370.04*m*Math.cos(lat/180.*Math.PI)*Math.sin(diffLon/180.*Math.PI);
//            y=-6370.04*m*Math.cos(lat/180.*Math.PI)*Math.cos(diffLon/180.*Math.PI);
//            System.out.println("y=-6370.04*"+m+"*"+Math.cos(lat/180.*Math.PI)+" * "+Math.cos(diffLon/180.*Math.PI));
            return new double[]{x, y};
        }
        throw new UnsupportedOperationException("Not yet implemented");

//        return new double[]{lat,lon};
    }

    /**
     *
     * @param lat
     * @param lon
     * @return (x,y) indizes starting with (0,0) in UPPER left corner of the map
     */
    public double[] getPositionIndicesForLatLon_DWD(double lat, double lon) {
        if (x == 900 && y == 900) {

            //
            double diffLat = lat - 60;//N  /  phi
            double diffLon = lon - 10;//E  /  lambda
            double m = (1 + Math.sin(60. / 180. * Math.PI)) / (1 + Math.sin(lat / 180. * Math.PI));
//            System.out.println("m(" + lat + ") = " + m);
            double x = 6370.04 * m * Math.cos(lat / 180. * Math.PI) * Math.sin(diffLon / 180. * Math.PI);
            double y = -6370.04 * m * Math.cos(lat / 180. * Math.PI) * Math.cos(diffLon / 180. * Math.PI);
//            System.out.println("y=-6370.04*" + m + "*" + Math.cos(lat / 180. * Math.PI) + " * " + Math.cos(diffLon / 180. * Math.PI));

            return new double[]{x + 523.4622, 900 - (y + 4658.645)};
        }
        throw new UnsupportedOperationException("Not yet implemented");

//        return new double[]{lat,lon};
    }

    public static void main1(String[] args) {
        double lowerleftLat, lowerleftLon, upperleftLat, upperleftLon, lowerRightLat, lowerRightLon, upperRightLat, upperRightLon;

        lowerleftLat = 46.9526;
        lowerleftLon = 3.5889;
        upperleftLat = 54.5877;
        upperleftLon = 2.0715;

        lowerRightLat = 47.0705;
        lowerRightLon = 14.6209;
        upperRightLat = 54.7405;
        upperRightLon = 15.7208;

        RadolanData data = new RadolanData(null, null, 900, 900, 1, null, lowerleftLat, lowerleftLon, upperleftLat, upperleftLon, lowerRightLat, lowerRightLon, upperRightLat, upperRightLon);

        double[] pos = data.getPositionIndicesForLatLon_DWD(upperRightLat, upperRightLon);
        System.out.println(pos[0] + " , " + pos[1]+"  upper right");

        pos = data.getPositionIndicesForLatLon_DWD(lowerRightLat, lowerRightLon);
        System.out.println(pos[0] + " , " + pos[1]+"  lower right");

        pos = data.getPositionIndicesForLatLon_DWD(52.39, 9.712296);
        System.out.println(pos[0] + " , " + pos[1] + " Hannover (DWD)");
        
        pos = data.getPositionIndicesForLatLon_RS(52.39, 9.712296);
        System.out.println(pos[0] + " , " + pos[1] + " Hannover (RS)");
    }

    /**
     *
     * @param lat
     * @param lon
     * @return (i,j) (row, column) in data
     */
    public double[] getDataIndicesForLatLon(double lat, double lon) {
        double[] xy = getPositionIndicesForLatLon_DWD(lat, lon);
        return new double[]{y - xy[1], xy[0]};
    }

    public int getValueIJ(int i, int j) {
        return values[i][j];
    }

    public int getValueXY(int x, int y) {
        return values[this.y - y - 1][x];
    }

}
