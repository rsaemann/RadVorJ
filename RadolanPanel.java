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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

/**
 * shows Map and information for Radolandata
 *
 * @author saemann
 */
public class RadolanPanel extends JPanel {

    private final JLabel label;
    private BufferedImage image;
    private RadolanData data;
    private int mouseX, mouseY, mouseValue;
    private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 15);

    public static int[] colorNaN = new int[]{100, 80, 80};
    public static int[] colorZero = new int[3];

    public RadolanPanel() {
        label = new JLabel("Placeholder for Picture");

        label.setToolTipText("Drag & Drop file here to show content.");
//        this.setLayout(new BorderLayout());
//        this.add(label, BorderLayout.CENTER);

//            System.out.println(data.createTextPicture(true));
        this.setTransferHandler(new TransferHandler(null) {
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
                    RadolanData d = RadolanReader.readFile(data.get(0));
                    setData(d);
//                    BufferedImage bi = d.createImage();
//                    Graphics g = bi.getGraphics();
//                    g.setColor(Color.magenta);
//                    g.fillOval(530, 900 - 603, 3, 3);
//                    ImageIcon ii = new ImageIcon(bi);
//                    label.setIcon(ii);
//
//                    label.setText(d.toHTMLString());
//
//                    frame.setTitle(data.get(0).getAbsolutePath());
//
//                    System.out.println(d.createTextPicture(true));
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

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent me) {
                Point point = me.getPoint();
                mouseX = point.x;
                mouseY = point.y;
                if (data != null) {
                    if (mouseX >= 0 && mouseX < data.x) {
                        if (mouseY >= 0 && mouseY < data.y) {
                            mouseValue = data.getValueXY(mouseX, mouseY);
                            repaint();
                        }
                    }
                }

            }

        });
    }

    public void setData(RadolanData data) {
        this.data = data;
        //Mark some cities in the picture
        image = createImage(data);
        Graphics g = image.getGraphics();
        g.setColor(Color.magenta);

        double[] dataXY = data.getPositionIndicesForLatLon(52.517892, 13.385468); //Berlin
        g.fillOval((int) (dataXY[0] - 2), (int) (dataXY[1] - 2), 6, 6);

        dataXY = data.getPositionIndicesForLatLon(52.380629, 9.727707); //Hannover
        g.drawOval((int) (dataXY[0] - 3), (int) (dataXY[1] - 3), 6, 6);
        //crosshair to show city without overriding the conent around
        g.drawLine(0, (int) (dataXY[1]), (int) (dataXY[0] - 20), (int) (dataXY[1]));
        g.drawLine((int) (dataXY[0] + 20), (int) (dataXY[1]), data.x, (int) (dataXY[1]));
        g.drawLine((int) (dataXY[0]), 0, (int) (dataXY[0]), (int) (dataXY[1] - 20));
        g.drawLine((int) (dataXY[0]), (int) (dataXY[1] + 20), (int) (dataXY[0]), data.y);

        if (mouseX >= 0 && mouseX < data.x) {
            if (mouseY >= 0 && mouseY < data.y) {
                mouseValue = data.getValueXY(mouseX, mouseY);
            }
        }

        repaint();
        // display picture on the frame
//        label.setIcon(new ImageIcon(bi));
        //Display information nect to the picture
//        label.setText(data.toHTMLString());
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs;
        try {
            g2.setColor(Color.white);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        } catch (Exception e) {
        }
        if (image != null) {
            g2.drawImage(image, 0, 0, this);
        }
        if (mouseValue >= 0) {
            g2.setFont(font);
            g2.setColor(Color.white);
            g2.drawString(mouseValue + "", mouseX, mouseY);
            g2.drawString(mouseValue + "", mouseX, mouseY - 2);
            g2.drawString(mouseValue + "", mouseX + 2, mouseY);
            g2.drawString(mouseValue + "", mouseX + 2, mouseY - 2);
            g2.setColor(Color.black);
            g2.drawString(mouseValue + "", mouseX + 1, mouseY - 1);
            g2.setColor(Color.magenta);
            g2.drawRect(mouseX - 1, mouseY - 1, 3, 3);

        } else if (mouseValue == Integer.MIN_VALUE) {
            g2.setFont(font);
            g2.setColor(Color.black);
            g2.drawString("NA", mouseX + 1, mouseY - 1);
            g2.setColor(Color.magenta);
            g2.drawRect(mouseX - 1, mouseY - 1, 3, 3);
        }

        g2.setColor(Color.black);
        g2.drawString("X:" + mouseX + ", Y:" + mouseY + ", i:" + (data.y - mouseY) + " j:" + mouseX + "    " + data.product + "  local:" + data.productionTime.getTime().toLocaleString() + "  " + ((data.leadTime > 0) ? ("+" + data.leadTime + " min") : "actual"), 3, data.y + 12);
    }

    public BufferedImage createImage(RadolanData data) {
        if (data.values == null) {
            throw new NullPointerException("No data values read.");
        }
        BufferedImage bi = new BufferedImage(data.values.length, data.values[0].length, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = bi.getRaster();
        for (int iy = 0; iy < data.values.length; iy++) {
            int yt = data.y - 1 - iy;//inverse Y for top down orientation
            for (int xt = 0; xt < data.values[0].length; xt++) {
                int value = data.values[iy][xt];
                if (value == Integer.MIN_VALUE) {
                    raster.setPixel(xt, yt, colorNaN);
                } else if (value == 0) {
                    raster.setPixel(xt, yt, colorZero);
                } else {
                    raster.setPixel(xt, yt, getColor(value));
//                    raster.setPixel(xt, yt, new int[]{(int) Math.min(255, Math.max(0, value > 0 ? 100 : 0) + value * 10), value > 0 ? 200 : 0, 0/*(int) Math.min(255, Math.max(0, (value*10)) )*/});
                }
            }
//            System.out.println(yt);
        }
        bi.setData(raster);

        return bi;
    }

    public int[] getColor(int value) {
        if (value == 0) {
            return colorZero;
        }
        if (value == Integer.MIN_VALUE) {
            return colorNaN;
        }
        int[] c;
        if (value <= 10) {
            //Blue->green
            c = new int[]{30, (int) (255 * value / 10), (int) (255 * (10 - value) / 10)};
        } else if (value <= 100) {
            //green->yellow
            c = new int[]{(int) (255 * (value - 10) / 90), 255, 0};
        } else if (value <= 400) {
            //yellow->red
            c = new int[]{255, (int) (255 * (300 - (value - 100)) / 300), 0};
        } else {
            c = new int[]{255, 0, 255};
        }
        //check range
        return new int[]{Math.max(0, Math.min(255, c[0])), Math.max(0, Math.min(255, c[1])), Math.max(0, Math.min(255, c[2]))};
    }

    public static void main(String[] args) {
        File file = new File("C:\\Users\\saemann\\Desktop\\RQ1907221600_000.gz");
        try {
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    System.out.println(arg);
                    File t = new File(arg);
                    if (t.exists()) {
                        file = t;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // File to load (bin/.gz/raw data)
        final JFrame frame = new JFrame("DWD Decoder    Robert Sämann 2019");
        try {
            RadolanPanel panel = new RadolanPanel();
            frame.add(panel);
            if (file.exists()) {
                panel.setData(RadolanReader.readFile(file));
            }

            double[] hannover = panel.data.getPositionIndicesForLatLon(52.39, 9.712296);
            System.out.println("Hannover: X:" + hannover[0] + " ,  y:" + hannover[1]);

            double[] ll = panel.data.getPositionIndicesForLatLon(46.9526, 3.5889);
            System.out.println("lowerleft: X:" + ll[0] + " ,  y:" + ll[1]);

            //Prepare frame size
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setBounds(100, 100, 900 + 200, 900 + 55);
            frame.setVisible(true);
        } catch (Exception ex) {
            Logger.getLogger(RadolanReader.class.getName()).log(Level.SEVERE, null, ex);
            frame.setTitle(ex.getLocalizedMessage());
        }
    }

}
