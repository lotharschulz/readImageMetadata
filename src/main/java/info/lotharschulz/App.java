package info.lotharschulz;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata.Item;
import org.apache.sanselan.common.RationalNumber;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.sanselan.formats.tiff.TiffImageData;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.metadata.IIOMetadata;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * 
 * @author: Lothar Schulz
 * inspired by: 
 *  http://commons.apache.org/sanselan/
 *  http://www.drewnoakes.com/code/exif/
 *  http://docs.oracle.com/javase/1.5.0/docs/guide/imageio/spec/apps.fm5.html
 *  http://openbook.galileodesign.de/javainsel5/javainsel14_008.htm#Rxx747java14008040004CF1F02222C
 *  http://johnbokma.com/java/obtaining-image-metadata.html
 *  http://www.screaming-penguin.com/node/7485
 *  http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=a8f4987dbb468ffffffffaaa38fb41a535b6?bug_id=4924909
 */
public class App {

    private static final Logger log = Logger.getLogger(App.class);
    private final static String filename = "jpeg.jpg";

    public static void main(String[] args) {

        File imgfile = new File(filename);

        Metadata drewmetadata = null;
        try {
            drewmetadata = ImageMetadataReader.readMetadata(imgfile);
        } catch (ImageProcessingException ipx) {
            String excStr = stackTraceToString(ipx);
            log.error(ipx.getMessage() + "\n" + excStr);
        } catch (IOException ioe) {
            String excStr = stackTraceToString(ioe);
            log.error(ioe.getMessage() + "\n" + excStr);
            System.exit(1);
        }
        if (drewmetadata != null) {
            for (Directory directory : drewmetadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    if (log.isDebugEnabled()) {
                        log.debug(tag);
                    }
                }
            }
            for (Directory directory : drewmetadata.getDirectories()) {
                if (log.isDebugEnabled()) {
                    log.debug("directory: " + directory);
                }
                for (Tag tag : directory.getTags()) {
                    if (log.isDebugEnabled()) {
                        log.debug("  tag: " + tag);
                    }
                }
            }
        }

        try {
            readSanselanMetadata(imgfile);
        } catch (ImageReadException ire) {
            String excStr = stackTraceToString(ire);
            log.error(ire.getMessage() + "\n" + excStr);
        } catch (IOException ioe) {
            String excStr = stackTraceToString(ioe);
            log.error(ioe.getMessage() + "\n" + excStr);
            System.exit(1);
        }

        readImageIOMetadata(filename);
    }

    public static void readSanselanMetadata(File file) throws ImageReadException,
            IOException {
        IImageMetadata sanselanmetadata = Sanselan.getMetadata(file);

        if (sanselanmetadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) sanselanmetadata;

            if (log.isDebugEnabled()) {
                log.debug("file: " + file.getPath());
            }

            printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION);
            printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
            printTagValue(jpegMetadata,
                    TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO);
            printTagValue(jpegMetadata,
                    TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE);
            printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
            printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE);
            printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);
            printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE);

            TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (null != exifMetadata) {
                TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
                if (null != gpsInfo) {
                    String gpsDescription = gpsInfo.toString();
                    double longitude = gpsInfo.getLongitudeAsDegreesEast();
                    double latitude = gpsInfo.getLatitudeAsDegreesNorth();

                    if (log.isDebugEnabled()) {
                        log.debug("    " + "GPS Description: " + gpsDescription);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("    " + "GPS Longitude (Degrees East): " + longitude);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("    " + "GPS Latitude (Degrees North): " + latitude);
                    }
                }
            }

            TiffField gpsLatitudeRefField = jpegMetadata.findEXIFValue(TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
            TiffField gpsLatitudeField = jpegMetadata.findEXIFValue(TiffConstants.GPS_TAG_GPS_LATITUDE);
            TiffField gpsLongitudeRefField = jpegMetadata.findEXIFValue(TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);
            TiffField gpsLongitudeField = jpegMetadata.findEXIFValue(TiffConstants.GPS_TAG_GPS_LONGITUDE);
            if (gpsLatitudeRefField != null && gpsLatitudeField != null
                    && gpsLongitudeRefField != null
                    && gpsLongitudeField != null) {
                String gpsLatitudeRef = (String) gpsLatitudeRefField.getValue();
                RationalNumber gpsLatitude[] = (RationalNumber[]) (gpsLatitudeField.getValue());
                String gpsLongitudeRef = (String) gpsLongitudeRefField.getValue();
                RationalNumber gpsLongitude[] = (RationalNumber[]) gpsLongitudeField.getValue();

                RationalNumber gpsLatitudeDegrees = gpsLatitude[0];
                RationalNumber gpsLatitudeMinutes = gpsLatitude[1];
                RationalNumber gpsLatitudeSeconds = gpsLatitude[2];

                RationalNumber gpsLongitudeDegrees = gpsLongitude[0];
                RationalNumber gpsLongitudeMinutes = gpsLongitude[1];
                RationalNumber gpsLongitudeSeconds = gpsLongitude[2];

                if (log.isDebugEnabled()) {
                    log.debug("    " + "GPS Latitude: "
                            + gpsLatitudeDegrees.toDisplayString() + " degrees, "
                            + gpsLatitudeMinutes.toDisplayString() + " minutes, "
                            + gpsLatitudeSeconds.toDisplayString() + " seconds "
                            + gpsLatitudeRef);
                }
                if (log.isDebugEnabled()) {
                    log.debug("    " + "GPS Longitude: "
                            + gpsLongitudeDegrees.toDisplayString() + " degrees, "
                            + gpsLongitudeMinutes.toDisplayString() + " minutes, "
                            + gpsLongitudeSeconds.toDisplayString() + " seconds "
                            + gpsLongitudeRef);
                }

            }


            if (log.isDebugEnabled()) {
                log.debug("jpegMetadata: " + jpegMetadata.toString());
            }
            printMetadataList(jpegMetadata.getItems());

            TiffImageMetadata tiffImageMetadata = jpegMetadata.getExif();
            if (null != tiffImageMetadata) {
                if (log.isDebugEnabled()) {
                    log.debug("tiffImageMetadata: " + tiffImageMetadata.toString());
                }
                printMetadataList(tiffImageMetadata.getItems());
            }

            JpegPhotoshopMetadata jpegPhotoshopMetadata = jpegMetadata.getPhotoshop();
            if (null != tiffImageMetadata) {
                if (log.isDebugEnabled()) {
                    log.debug("jpegPhotoshopMetadata: " + jpegPhotoshopMetadata.toString());
                }
                printMetadataList(jpegPhotoshopMetadata.getItems());
            }

            TiffImageData tiffImageData = null;
            try {
                tiffImageData = jpegMetadata.getRawImageData();
            } catch (NullPointerException npe) {
                String excStr = stackTraceToString(npe);
                log.error(npe.getMessage() + "\n" + excStr);
            }
            if (null != tiffImageData) {
                if (log.isDebugEnabled()) {
                    log.debug("tiffImageData: " + tiffImageData.toString());
                }
                printMetadataArray(tiffImageData.getImageData());
            }

        }
    }

    public static void printMetadataList(List items) {
        Object item;
        for (int i = 0; i < items.size(); i++) {
            item = items.get(i);
            if (log.isDebugEnabled()) {
                log.debug("    " + "item: " + item + " - class: " + item.getClass());
            }
            if (item instanceof org.apache.sanselan.common.ImageMetadata.Item) {
                Item tf_item = (Item) item;
                if (log.isDebugEnabled()) {
                    log.debug("       " + tf_item.getKeyword() + " - " + tf_item.getText());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("          item: " + item.toString());
                }
            }
        }
    }

    public static void printMetadataArray(Object[] elems) {
        Object item;
        for (int i = 0; i < elems.length; i++) {
            item = elems[i];
            if (log.isDebugEnabled()) {
                log.debug("    " + "item: " + item + " - class: " + item.getClass());
            }
        }
    }

    public static void printTagValue(JpegImageMetadata jpegMetadata,
            TagInfo tagInfo) {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field == null) {
            if (log.isDebugEnabled()) {
                log.debug(tagInfo.name + ": " + "Not Found.");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(tagInfo.name + ": "
                        + field.getValueDescription());
            }
        }
    }

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void readImageIOMetadata(String file) {
        ImageInputStream iis = null;
        boolean iisclosed = false;
        try {
            iis = ImageIO.createImageInputStream(new BufferedInputStream(new FileInputStream(file)));

            Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
            IIOImage image = null;
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true);
                try {
                    image = reader.readAll(0, null);
                } catch (javax.imageio.IIOException iioex) {
                    String excStr = stackTraceToString(iioex);
                    log.error(iioex.getMessage() + "\n" + excStr);
                    if (iioex.getMessage() != null
                            && iioex.getMessage().endsWith("without prior JFIF!")) {
                        log.warn("Trying workaround for java bug");
                        log.warn("http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4924909");
                        log.warn("Please vote for this bug!");
                        iis.close();
                        iisclosed = true;
                    }
                } catch (IOException ioe) {
                    String excStr = stackTraceToString(ioe);
                    log.error(ioe.getMessage() + "\n" + excStr);
                    System.exit(1);
                }
                if (null != image) {
                    IIOMetadata metadata = image.getMetadata();
                    String[] names = metadata.getMetadataFormatNames();
                    for (int i = 0; i < names.length; i++) {
                        if (log.isDebugEnabled()) {
                            log.debug("Format name: " + names[ i]);
                            log.debug(displayMetadata(metadata.getAsTree(names[i])));
                        }
                    }
                }
            }
            if(!iisclosed) iis.close();
        } catch (IOException ioe) {
            String excStr = stackTraceToString(ioe);
            log.error(ioe.getMessage() + "\n" + excStr);
            System.exit(1);
        }


    }

    public static String displayMetadata(Node node) {
        String outp = "<" + node.getNodeName();
        NamedNodeMap map = node.getAttributes();
        if (map != null) {

            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                outp = outp + " " + attr.getNodeName()
                        + "=\"" + attr.getNodeValue() + "\"";
            }
        }

        Node child = node.getFirstChild();
        if (child == null) {
            outp = outp + "/>\n";
            return outp;
        }

        outp = outp + ">\n";
        while (child != null) {
            outp = outp + displayMetadata(child);
            child = child.getNextSibling();
        }
        outp = outp + "</" + node.getNodeName() + ">\n";
        return outp;
    }
}