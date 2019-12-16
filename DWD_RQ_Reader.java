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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import rain.Rain_Reader;

/**
 * Downloads data from DWD and decodes gzip files.
 *
 * @author saemann
 */
public class DWD_RQ_Reader implements Rain_Reader {

    public static String urlRootRQ = "https://opendata.dwd.de/weather/radar/radvor/rq/";

    public static long updateMS = 16 * 60 * 1000;

    public long nextUpdate;

    private String actualFileName, fc60FileName, fc120FileName;

    public File fileStoreDirectoryDownloads = new File("L:\\WetterDWDForecast");

    public long productionTime = 0;

    /**
     * Saving in archive as encoded file saves a lot of disc space.
     */
    public boolean saveGZIPEncoded = true;

    /**
     * Indices where to read the weather information.
     */
    public int i, j;

    public static DWD_RQ_Reader DWD_RQ_Reader_Hannover_Ricklingen() {
        DWD_RQ_Reader reader = new DWD_RQ_Reader();
        reader.i = 611;
        reader.j = 503;
        return reader;
    }

    @Override
    public double[][] readRain() {
        double[][] rain = new double[3][2];
        //Download Data
        boolean useTemp = true;
        if (fileStoreDirectoryDownloads != null && fileStoreDirectoryDownloads.exists() && fileStoreDirectoryDownloads.canWrite()) {
            useTemp = false;
        }

        //Download actual file
        File fileActual = null;
        if (actualFileName != null) {
            try {
                boolean alreadyDownloaded = false;
                if (useTemp) {
                    fileActual = File.createTempFile(actualFileName.replace(".gz", ""), "");
                } else {
                    if (saveGZIPEncoded) {
                        fileActual = new File(fileStoreDirectoryDownloads, actualFileName);
                    } else {
                        fileActual = new File(fileStoreDirectoryDownloads, actualFileName.replace(".gz", ""));
                    }
                    if (fileActual.exists() && fileActual.length() > 10) {
                        alreadyDownloaded = true;
                    }
                }
                if (!alreadyDownloaded) {
                    if (useTemp || !saveGZIPEncoded) {
                        downloaddecoded(urlRootRQ + actualFileName, fileActual);
                    } else {
                        downloadencoded(urlRootRQ + actualFileName, fileActual);
                    }
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyManagementException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (fileActual != null && fileActual.exists() && fileActual.canRead()) {
            try {
                RadolanData dataactual;
                if (useTemp || !saveGZIPEncoded) {
                    dataactual = RadolanReader.readRawData(fileActual);
                } else {
                    dataactual = RadolanReader.readFile(fileActual);
                }
                int intvalue = dataactual.getValueIJ(i, j);
                double rainMMpH = intvalue * 0.1;
                rain[0][0] = dataactual.productionTime.getTimeInMillis();
                rain[0][1] = rainMMpH;
                this.productionTime = dataactual.productionTime.getTimeInMillis();
            } catch (IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //Download 60 min forecast file
        File file60 = null;
        if (fc60FileName != null) {
            try {
                boolean alreadyDownloaded = false;
                if (useTemp) {
                    file60 = File.createTempFile(fc60FileName.replace(".gz", ""), "");
                } else {
                    if (saveGZIPEncoded) {
                        file60 = new File(fileStoreDirectoryDownloads, fc60FileName);
                    } else {
                        file60 = new File(fileStoreDirectoryDownloads, fc60FileName.replace(".gz", ""));
                    }
                    if (file60.exists() && file60.length() > 10) {
                        alreadyDownloaded = true;
                    }
                }
                if (!alreadyDownloaded) {
                    if (useTemp || !saveGZIPEncoded) {
                        downloaddecoded(urlRootRQ + fc60FileName, file60);
                    } else {
                        downloadencoded(urlRootRQ + fc60FileName, file60);
                    }
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException | KeyManagementException | IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (file60 != null && file60.exists() && file60.canRead()) {
            try {
                RadolanData data;
                if (useTemp || !saveGZIPEncoded) {
                    data = RadolanReader.readRawData(file60);
                } else {
                    data = RadolanReader.readFile(file60);
                }
                int intvalue = data.getValueIJ(i, j);
                double rainMMpH = intvalue * 0.1;
                rain[1][0] = data.productionTime.getTimeInMillis() + 60 * 60 * 1000;
                rain[1][1] = rainMMpH;
            } catch (IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //Download 120 min forecast file
        File file120 = null;
        if (fc120FileName != null) {
            try {
                boolean alreadyDownloaded = false;
                if (useTemp) {
                    file120 = File.createTempFile(fc120FileName.replace(".gz", ""), "");
                } else {
                    if (saveGZIPEncoded) {
                        file120 = new File(fileStoreDirectoryDownloads, fc120FileName);
                    } else {
                        file120 = new File(fileStoreDirectoryDownloads, fc120FileName.replace(".gz", ""));
                    }
                    if (file120.exists() && file120.length() > 10) {
                        alreadyDownloaded = true;
                    }
                }
                if (!alreadyDownloaded) {
                    if (useTemp || !saveGZIPEncoded) {
                        downloaddecoded(urlRootRQ + fc120FileName, file120);
                    } else {
                        downloadencoded(urlRootRQ + fc120FileName, file120);
                    }
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException | KeyManagementException | IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (file120 != null && file120.exists() && file120.canRead()) {
            try {
                RadolanData data;
                if (useTemp || !saveGZIPEncoded) {
                    data = RadolanReader.readRawData(file120);
                } else {
                    data = RadolanReader.readFile(file120);
                }
                int intvalue = data.getValueIJ(i, j);
                double rainMMpH = intvalue * 0.1;
                rain[2][0] = data.productionTime.getTimeInMillis() + 120 * 60 * 1000;
                rain[2][1] = rainMMpH;// this is only for the time period +60 to +120 minutes .     Wrong:- rain[1][1]; //only put the difference between 120 minute and 60minute commulative forecast here.
            } catch (IOException ex) {
                Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return rain;
    }

    public void downloaddecoded(String url, File decodedFile) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL myurl = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, null, new SecureRandom());
        con.setSSLSocketFactory(sslContext.getSocketFactory());
        con.setReadTimeout(1000);

        GZIPInputStream gZIPInputStream = new GZIPInputStream(con.getInputStream());
        FileOutputStream fileOutputStream = new FileOutputStream(decodedFile);

        int bytes_read;
        byte[] buffer = new byte[1024];
        while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, bytes_read);
        }
        gZIPInputStream.close();
        fileOutputStream.close();
        con.disconnect();
    }

    public void downloadencoded(String url, File decodedFile) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL myurl = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, null, new SecureRandom());
        con.setSSLSocketFactory(sslContext.getSocketFactory());
        con.setReadTimeout(1000);

        InputStream inputStream = con.getInputStream();
        FileOutputStream fileOutputStream = new FileOutputStream(decodedFile);

        int bytes_read;
        byte[] buffer = new byte[1024];
        while ((bytes_read = inputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, bytes_read);
        }
        inputStream.close();
        fileOutputStream.close();
        con.disconnect();
    }

    @Override
    public boolean newDataAvailable() {
        return System.currentTimeMillis() > nextUpdate;
    }

    public void checkForNewData() {
        try {
            boolean newVersion = true;
            //URL to RQ product index
            String httpsURL = "https://opendata.dwd.de/weather/radar/radvor/rq/";

            URL myurl = new URL(httpsURL);

            HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, new SecureRandom());
            con.setSSLSocketFactory(sslContext.getSocketFactory());
            con.setReadTimeout(1000);

            InputStream ins = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
//            System.out.println("cipher: " + con.getCipherSuite());
            String[] actual = new String[3];
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("</pre><hr>")) {
                    //Get out, when the end of content is reached.
                    break;
                }
                actual[2] = actual[1];
                actual[1] = actual[0];
                actual[0] = inputLine;
            }
            in.close();

            String line = actual[0].replaceAll("\\s++", " ");
            String[] newestParts = line.split(" ");
//            System.out.println("newsetparts[0]='"+newestParts[0]+"',   parts:"+newestParts.length+"  line='"+line+"'");
            String fileLink = newestParts[1].substring(newestParts[1].indexOf("\"") + 1, newestParts[1].lastIndexOf("\""));
            String date = newestParts[2];
            String time = newestParts[3];
            String length = newestParts[4];

//            System.out.println("Found newest: " + fileLink + " @" + date + ", " + time + ", " + length + " bytes");
            if (actualFileName != null) {
                if (actualFileName.contains(fileLink.substring(0, 12))) {
                    newVersion = false;
                }
            }

            if (fileLink.endsWith("_120.gz")) {
                fc120FileName = fileLink;
                fc60FileName = fileLink.replace("_120.gz", "_060.gz");
                actualFileName = fileLink.replace("_120.gz", "_000.gz");
            } else if (fileLink.endsWith("_060.gz")) {
                fc60FileName = fileLink;
                fc120FileName = fileLink.replace("_060.gz", "_120.gz");
                actualFileName = fileLink.replace("_060.gz", "_000.gz");
            } else if (fileLink.endsWith("_000.gz")) {
                actualFileName = fileLink;
                fc120FileName = fileLink.replace("_000.gz", "_120.gz");
                fc60FileName = fileLink.replace("_000.gz", "_060.gz");
            } else {
                System.err.println("Do not understand file name '" + fileLink + "'");
            }

//            if (newVersion) {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.YEAR, Integer.parseInt(fileLink.substring(2, 4)) + 2000);
            cal.set(Calendar.MONTH, Integer.parseInt(fileLink.substring(4, 6)) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(fileLink.substring(6, 8)));

            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(fileLink.substring(8, 10)));
            cal.set(Calendar.MINUTE, Integer.parseInt(fileLink.substring(10, 12)));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            //Now override this information with the upload time from the website, because the upload is ~4 minutes after the filegeneration
            String[] timeparts = time.split(":");
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeparts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(timeparts[1]));

//                System.out.println("upload:" + cal.getTime());
            this.nextUpdate = cal.getTimeInMillis() + updateMS;
//                System.out.println("update:" + new Date(nextUpdate));
//            }    

        } catch (IOException ex) {
            Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(DWD_RQ_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main1(String[] args) {
        DWD_RQ_Reader dwd = DWD_RQ_Reader_Hannover_Ricklingen();
        System.out.println("New Data available? " + dwd.newDataAvailable());
        dwd.checkForNewData();
        double[][] rain = dwd.readRain();
        for (int i = 0; i < rain.length; i++) {
            System.out.println(new Date((long) rain[i][0]) + ":  " + rain[i][1] + "mm/h");

        }
    }

}
