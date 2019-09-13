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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * Decodes DWD Radolan Data.
 * 
 * Spezifikation:
 * https://www.dwd.de/DE/leistungen/radolan/radolan_info/radolan_radvor_op_komposit_format_pdf.pdf?__blob=publicationFile&v=11
 *
 * @author saemann
 */
public class RadolanReader {

    /**
     * If file is zipped, it is decrypted to temp-directory first and
     * readRawData afterwards.
     *
     * @param gzipFile
     * @return
     * @throws IOException
     */
    public static RadolanData readFile(File gzipFile) throws IOException {
        byte[] buffer = new byte[1024];
        //Create temporary file
        File decompressedFile;
        boolean deletatEnd = false;
        if (gzipFile.getName().endsWith(".gz")) {
            //FIle needs to be decrypted first.
            decompressedFile = File.createTempFile(gzipFile.getName(), "");
            decompressedFile.deleteOnExit();
            try {

                FileInputStream fileIn = new FileInputStream(gzipFile);

                GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);

                FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);

                int bytes_read;

                while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {

                    fileOutputStream.write(buffer, 0, bytes_read);
                }
                deletatEnd = true;
                gZIPInputStream.close();
                fileOutputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            decompressedFile = gzipFile;
        }
        RadolanData r = readRawData(decompressedFile);
        if (deletatEnd) {
            try {
                decompressedFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return r;

    }

    public static RadolanData readRawData(File f) throws IOException {

        FileInputStream fis = new FileInputStream(f);
        InputStreamReader sr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(sr);

        String line = br.readLine();
//        System.out.println(line);
        //Read header
        String product = line.substring(0, 2);
        int day = Integer.parseInt(line.substring(2, 4));
        int hour = Integer.parseInt(line.substring(4, 6));
        int minute = Integer.parseInt(line.substring(6, 8));
        String radarID = line.substring(8, 13);
        int month = Integer.parseInt(line.substring(13, 15));
        int year = Integer.parseInt(line.substring(15, 17));

        int markeBY = line.indexOf("BY");
        if (!line.substring(17, 19).equals("BY")) {
            System.err.println("Exception: BY not at location 17");
            return null;
        }
        //Create UTC time object (DWD data is in UTC timezone format)
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(GregorianCalendar.YEAR, year + 2000);
        cal.set(GregorianCalendar.MONTH, month - 1);
        cal.set(GregorianCalendar.DAY_OF_MONTH, day);
        cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
        cal.set(GregorianCalendar.MINUTE, minute);
        cal.set(GregorianCalendar.SECOND, 0);
        cal.set(GregorianCalendar.MILLISECOND, 0);

        int markeVS = line.indexOf("VS");
        int contentLength = Integer.parseInt(line.substring(markeBY + 2, markeVS));

        int markeSW = line.indexOf("SW", markeBY);
        int markeINT = line.indexOf("INT", markeSW);
        int markeGP = line.indexOf("GP", markeINT);
        int markeVV = line.indexOf("VV", markeGP);
        int markeMF = line.indexOf("MF", markeVV);
        int markeMS = line.indexOf("MS", markeMF);

        int lengthText = Integer.parseInt(line.substring(markeMS + 2, line.indexOf("<", markeMS)).trim());
        int markeST = line.indexOf("ST", markeMS + lengthText);

        int markeETX = line.indexOf('\u0003');
        if (markeETX < 0) {
            markeETX = line.indexOf("\\x03");
        }
        //Grid size

        String grid;
        if (markeVV < 0) {
            if (markeMF < 0) {
                grid = line.substring(markeGP + 2, markeMS);
            } else {
                grid = line.substring(markeGP + 2, markeMF);
            }
        } else {
            grid = line.substring(markeGP + 2, markeVV);
        }
        int posX = grid.indexOf("x");
        int x = Integer.parseInt(grid.substring(0, posX).trim());
        int y = Integer.parseInt(grid.substring(posX + 1).trim());

        //Forecast time (lead time) [Minutes]
        int leadTime;
        if (markeVV < 0) {
            leadTime = -1;
        } else {
            leadTime = Integer.parseInt(line.substring(markeVV + 2, markeMF).trim());
        }

//        System.out.println("Grid:" + x + "x " + y);
//        System.out.println("Leadtime:" + leadTime + "min");
//        System.out.println("Information: " + contentLength + " byte");

        br.close();
        sr.close();
        fis.close();

        fis = new FileInputStream(f);

        //Read value grid
        byte[] buffer = new byte[contentLength];
        //Jump to content start point
        fis.skip(markeETX + 2);
        fis.read(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.BIG_ENDIAN);
        int[][] values = new int[y][x];
        //Values are stored horizontal line-wise
        for (int i = 0; i < y; i++) {
            for (int j = 0; j < x; j++) {
                //decode 2-byte content
                int complete = bb.getShort();

                //Value only in bit 1-12
                int v = complete & 0x0FFF;

                if (v != 0) {
                    if ((complete & 0x8000) != 0) {
                        //Clutter Mark
                    }
                    if ((complete & 0x2000) != 0) {
                        //Error mark
                        v = Integer.MIN_VALUE;
                    }
                    if ((complete & 0x4000) != 0) {
                        //Negative value
                        v = -v;
                    }
                }
                values[i][j] = v;
            }
        }

        double lowerleftLat, lowerleftLon, upperleftLat, upperleftLon, lowerRightLat, lowerRightLon, upperRightLat, upperRightLon;
        if (x == 900 && y == 900) {
            lowerleftLat = 46.9526;
            lowerleftLon = 3.5889;
            upperleftLat = 54.5877;
            upperleftLon = 2.0715;

            lowerRightLat = 47.0705;
            lowerRightLon = 14.6209;
            upperRightLat = 54.7405;
            upperRightLon = 15.7208;
        } else if (x == 900 && y == 1100) {
            lowerleftLat = 46.1929;
            lowerleftLon = 4.6759;
            upperleftLat = 55.5482;
            upperleftLon = 3.0889;

            lowerRightLat = 46.1827;
            lowerRightLon = 15.4801;
            upperRightLat = 55.5342;
            upperRightLon = 17.1128;
        } else {
            System.err.println("Do not know corners for x=" + x + " , y=" + y + " grid.");
            lowerleftLat = 46.9526;
            lowerleftLon = 3.5889;
            upperleftLat = 54.5877;
            upperleftLon = 2.0715;

            lowerRightLat = 47.0705;
            lowerRightLon = 14.6209;
            upperRightLat = 54.7405;
            upperRightLon = 15.7208;
        }
        return new RadolanData(product, values, x, y, leadTime, cal, lowerleftLat, lowerleftLon, upperleftLat, upperleftLon, lowerRightLat, lowerRightLon, upperRightLat, upperRightLon);
    }

    /**
     * Helping method to display a bytebuffers content.
     *
     * @param bytes
     * @return
     */
    public static String byteString(byte[] bytes) {
        StringBuilder str = new StringBuilder(bytes.length * 20);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            str.append((b & 0x80) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x40) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x20) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x10) > 0 ? "1" : "0").append("  ");
            str.append((b & 0x08) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x04) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x02) > 0 ? "1" : "0").append(" ");
            str.append((b & 0x01) > 0 ? "1" : "0").append("    ");
        }
        return str.toString();
    }

  
}
