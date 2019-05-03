package com.ca.cdd.plugins.gradletesting.utils;

import org.gradle.internal.SystemProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PrefixedOutputStream extends OutputStream {

    private final OutputStream out;
    private final byte lastLineSeparatorByte;
    private final byte[] prefix;

    public PrefixedOutputStream(String prefix, OutputStream out) {
        this.out = out;
        byte[] lineSeparator = SystemProperties.getInstance().getLineSeparator().getBytes(StandardCharsets.UTF_8);
        this.lastLineSeparatorByte = lineSeparator[lineSeparator.length - 1];
        this.prefix = prefix.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        if ( endsWithLineSeparator(b) ) {
            for (short i=0; i<prefix.length; i++) {
                out.write(prefix[i]);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /* **************************************************************** */
    /* COPIED FROM org.gradle.internal.io.LineBufferingOutputStream !!! */
    /* **************************************************************** */

    // only check for the last byte of a multi-byte line separator
    // besides this, always check for '\n'
    // this handles '\r' (MacOSX 9), '\r\n' (Windows) and '\n' (Linux/Unix/MacOSX 10)
    private boolean endsWithLineSeparator(int b) {
        byte currentByte = (byte) (b & 0xff);
        return currentByte == lastLineSeparatorByte || currentByte == '\n';
    }

}
