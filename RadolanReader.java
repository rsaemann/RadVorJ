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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

/**
 *
 * Spezifikation:
 * https://www.dwd.de/DE/leistungen/radolan/radolan_info/radolan_radvor_op_komposit_format_pdf.pdf?__blob=publicationFile&v=11
 *
 * @author saemann
 */
public class RadolanReader {

    String product;
    int[][] values;
    private int x;
    private int y;
    private int leadTime;
    private GregorianCalendar cal;

    public void read(File gzipFile) throws IOException {
        byte[] buffer = new byte[1024];
        //Create temporary file
        File decompressedFile;

        if (gzipFile.getName().endsWith(".gz")) {
            decompressedFile = File.createTempFile(gzipFile.getName(), "");

            try {

                FileInputStream fileIn = new FileInputStream(gzipFile);

                GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);

                FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);

                int bytes_read;

                while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {

                    fileOutputStream.write(buffer, 0, bytes_read);
                }

                gZIPInputStream.close();
                fileOutputStream.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            decompressedFile = gzipFile;
        }
        FileInputStream fis = new FileInputStream(decompressedFile);
        InputStreamReader isr = new InputStreamReader(fis);

        read(isr, fis);
        isr.close();
        fis.close();

    }

    public void read(InputStreamReader stream, InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(stream);
        br.mark(100);
        String line = br.readLine();
//        System.out.println(line);
        //Read header
        product = line.substring(0, 2);
        int day = Integer.parseInt(line.substring(2, 4));
        int hour = Integer.parseInt(line.substring(4, 6));
        int minute = Integer.parseInt(line.substring(6, 8));
        String radarID = line.substring(8, 13);
        int month = Integer.parseInt(line.substring(13, 15));
        int year = Integer.parseInt(line.substring(15, 17));

        int markeBY = line.indexOf("BY");
        if (!line.substring(17, 19).equals("BY")) {
            System.err.println("Exception: BY not at location 17");
            return;
        }
        //Create UTC time object (DWD data is in UTC timezone format)
        cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
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
        if (markeST < 0) {
//            System.err.println("no ST marking.");
        }
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
        x = Integer.parseInt(grid.substring(0, posX).trim());
        y = Integer.parseInt(grid.substring(posX + 1).trim());

        //Forecast time (lead time) [Minutes]
        if (markeVV < 0) {
            leadTime = -1;
        } else {
            leadTime = Integer.parseInt(line.substring(markeVV + 2, markeMF).trim());
        }

//        System.out.println(line);
//        System.out.println(cal);
//        System.out.println(cal.getTime());
        System.out.println("Grid:" + x + "x " + y);
        System.out.println("Leadtime:" + leadTime + "min");
        System.out.println("Information: " + contentLength + " byte");
//        System.out.println("start data at pointer" + markeETX);

        //Read values
        byte[] buffer = new byte[contentLength];

        is.skip(markeETX + 2);
        is.read(buffer);

        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.BIG_ENDIAN);
        StringBuilder str = new StringBuilder(2000);
        this.values = new int[y][x];
        int count = 0;
        int lastX=-1,lastY=-1;
        for (int i = 0; i < x; i++) {
            str = new StringBuilder(2000);

            for (int j = 0; j < y; j++) {
                //decode 2-byte content
                int pos = bb.position();
//                int s = bb.get();//steuerbits
//                int v = bb.get();
//                int addition = (s & 0x0F) << 8;
                int complete = bb.getShort();

                int v = complete & 0x0FFF;

//                ByteBuffer tbb=ByteBuffer.wrap(bbuffer);
//                tbb.order(ByteOrder.BIG_ENDIAN);
//                int cc=tbb.getShort();
//                v=cc&0x0FFF;
                if (v != 0) {
//                    System.out.println(byteString(bbuffer)+" // "+byteString(tbb.array())+" wert: "+v+"   ,  "+(cc&0x0FFF));//+"  addiditon:"+addition+" = "+(v+addition));
//                    v+=addition;
//                    if((complete&0xF000)!=0){
//                        System.out.println("Sonderfall "+byteString(bbuffer));
//                    }
                    byte[] bbuffer = new byte[2];
                    bb.position(pos);
                    bb.get(bbuffer);
//                if(addition>0)System.out.println(addition+" wert: "+v);
                    if ((complete & 0x8000) != 0) {
                        System.out.println(byteString(bbuffer));
                        System.out.println(complete + " clutter in " + count);
                    }
                    if ((complete & 0x2000) != 0) {
//                        System.out.println(byteString(bbuffer));
//                        System.out.println("Fehlkennung");
                        v = Integer.MIN_VALUE;
                        lastX=i;
                        lastY=j;
                    }
                    if ((complete & 0x4000) != 0) {
                        System.out.println(byteString(bbuffer));
                        System.out.println("Negatives Vorzeichen");
                        v = -v;
                    }
                }
                if (v > 0) {
                    str.append(",");
                } else if(v==Integer.MIN_VALUE){
                    str.append("W");
                }else{
                    str.append(" ");
                }
                this.values[i][j] = v;
                count++;
            }
            System.out.println(str.toString());

        }
        System.out.println("Counted " + count);
        
        //reorder values
        System.out.println("lastMinvalue: "+lastX+", "+lastY);
        if((lastX>0&&lastX<x-1)||(lastY>0&&lastY<y-1)){
            System.out.println("Reorder values");
            int[][] newValues=new int[values.length][values[0].length];
            for (int i = 0; i < newValues.length; i++) {
                for (int j = 0; j < newValues[i].length; j++) {
                    newValues[i][j]=values[(i+lastX)%values.length][(j+lastY)%values[0].length];                    
                }
            }
            this.values=newValues;
        }
        
        
        if (true) {
            return;
        }

        int i = 0;
        while (br.ready()) {
            line = br.readLine();
            System.out.println(i + ")" + line);
            i++;
        }
    }

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

    public void read2(InputStreamReader stream) throws IOException {
        BufferedReader br = new BufferedReader(stream);
        br.mark(100);
        String line = br.readLine();
        //Read header
        product = line.substring(0, 2);
        int day = Integer.parseInt(line.substring(2, 4));
        int hour = Integer.parseInt(line.substring(4, 6));
        int minute = Integer.parseInt(line.substring(6, 8));
        String radarID = line.substring(8, 13);
        int month = Integer.parseInt(line.substring(13, 15));
        int year = Integer.parseInt(line.substring(15, 17));

        int markeBY = line.indexOf("BY");
        if (!line.substring(17, 19).equals("BY")) {
            System.err.println("Exception: BY not at location 17");
            return;
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
        int contentLength = Integer.parseInt(line.substring(markeBY, markeVS));

        int markeSW = line.indexOf("SW", markeBY);
        int markeINT = line.indexOf("INT", markeSW);
        int markeGP = line.indexOf("GP", markeINT);

        System.out.println(line);
        System.out.println(cal);
        System.out.println(cal.getTime());

        if (true) {
            return;
        }

        int i = 0;
        while (br.ready()) {
            line = br.readLine();
            System.out.println(i + ")" + line);
            i++;
        }
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
                    raster.setPixel(xt, yt, new int[]{50, 50, 50});
                } else {
                    raster.setPixel(xt, yt, new int[]{(int) Math.min(255, Math.max(0, value > 0 ? 100 : 0) + value * 10), value > 0 ? 200 : 0, 0/*(int) Math.min(255, Math.max(0, (value*10)) )*/});
                }
            }
        }
        bi.setData(raster);

        return bi;
    }

    public static void main(String[] args) {
        try {
            File file = new File("C:\\Users\\saemann\\Desktop\\RQ1907221600_000.gz");

            final RadolanReader rr = new RadolanReader();
            rr.read(file);

            final JFrame frame = new JFrame(file.getAbsolutePath());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setBounds(100, 100, 1000, 1000);
            frame.setVisible(true);
            final JLabel label = new JLabel("Label");
            frame.setLayout(new BorderLayout());
            frame.add(label, BorderLayout.CENTER);
            label.setIcon(new ImageIcon(rr.createImage()));
            label.setText("<html>" + file.getName() + "<br>" + rr.cal.getTime().toGMTString() + "<br>" + rr.x + " x " + rr.y + "<br>" + rr.leadTime + "min lead</html>");

            label.setTransferHandler(new TransferHandler(null) {
                @Override
                public boolean canImport(TransferHandler.TransferSupport info) {
                    // we only import FileList
                    if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean importData(TransferHandler.TransferSupport info) {
                    if (!info.isDrop()) {
                        return false;
                    }

                    // Check for FileList flavor
                    if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        displayDropLocation("List doesn't accept a drop of this type.");
                        return false;
                    }
                    label.setIcon(null);
                    // Get the fileList that is being dropped.
                    Transferable t = info.getTransferable();
                    List<File> data;
                    try {
                        data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        rr.read(data.get(0));
                        BufferedImage bi = rr.createImage();

                        ImageIcon ii = new ImageIcon(bi);
                        label.setIcon(ii);
//                    label.setText("Drag & Drop file here to show content.");
                        label.setText("<html>" + data.get(0).getName() + "<br>" + rr.cal.getTime().toGMTString() + "<br>" + rr.x + " x " + rr.y + "<br>" + rr.leadTime + "min lead</html>");

                        frame.setTitle(data.get(0).getAbsolutePath());
                    } catch (Exception e) {
                        label.setIcon(null);
                        label.setText(e.getLocalizedMessage());
                        e.printStackTrace();
                        return false;
                    }

                    return true;
                }

                private void displayDropLocation(String string) {
                    System.out.println(string);
                }
            });

        } catch (IOException ex) {
            Logger.getLogger(RadolanReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
