package ubc.pavlab.gotrack.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

public class Zipper {

    public static byte[] zip( String data, Charset charset ) {
        try {
            byte[] dataToCompress = data.getBytes( charset );

            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream( dataToCompress.length );
            try {
                try (GZIPOutputStream zipStream = new GZIPOutputStream( byteStream )) {
                    zipStream.write( dataToCompress );
                }
            } finally {
                byteStream.close();
            }

            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new byte[]{};

    }
}