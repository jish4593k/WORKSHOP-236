import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import com.itextpdf.text.pdf.codec.TiffWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoveWatermark {

    private static boolean isGray(int a, int b, int c) {
        int r = 40;
        if (a + b + c < 350) {
            return true;
        }
        if (Math.abs(a - b) > r) {
            return false;
        }
        if (Math.abs(a - c) > r) {
            return false;
        }
        return Math.abs(b - c) <= r;
    }

    private static Image removeWatermark(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image newImage = new Image(width, height, Image.TYPE_INT_RGB);
        int[] pixels = new int[width * height];

        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;

            if (isGray(red, green, blue)) {
                pixels[i] = 0xFFFFFF; // White color
            }
        }

        newImage.setRGB(0, 0, width, height, pixels, 0, width);
        return newImage;
    }

    private static void processPage(RandomAccessFileOrArray pdf, int pageIndex, boolean skipped) {
        try {
            TiffImage tiffImage = TiffImage.getTiffImage(pdf, pageIndex + 1);
            Image image = TiffWriter.getImage(tiffImage);
            if (!skipped) {
                image = removeWatermark(image);
            }

            FileOutputStream fos = new FileOutputStream("./temp/" + pageIndex + ".jpg");
            Image.writeImage(image, "jpg", fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String inputPdfPath = "path/to/input.pdf";
        String outputPdfPath = "path/to/output.pdf";
        int skip = 0;

        Logger logger = Logger.getLogger(RemoveWatermark.class.getName());
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPdfPath));
            document.open();

            for (int i = 0; i < 10; i++) {  // Assuming 10 pages in the PDF
                logger.log(Level.INFO, "Processing page " + (i + 1) + "/10");
                processPage(new RandomAccessFileOrArray(inputPdfPath), i, i < skip);
            }

            logger.log(Level.INFO, "Writing to output PDF file");
            for (int i = 0; i < 10; i++) {  // Assuming 10 pages in the PDF
                Image image = Image.getInstance("./temp/" + i + ".jpg");
                document.add(image);
            }

            logger.log(Level.INFO, "Done");
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
