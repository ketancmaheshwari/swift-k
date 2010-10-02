//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jan 1, 2010
 */
package org.globus.cog.abstraction.impl.file.coaster.handlers.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.file.coaster.buffers.Buffers;
import org.globus.cog.abstraction.impl.file.coaster.buffers.ReadBuffer;
import org.globus.cog.abstraction.impl.file.coaster.buffers.ReadBufferCallback;
import org.globus.cog.abstraction.impl.file.coaster.buffers.WriteBuffer;
import org.globus.cog.abstraction.impl.file.coaster.buffers.WriteBufferCallback;
import org.globus.cog.abstraction.impl.file.coaster.handlers.CoasterFileRequestHandler;

public class LocalIOProvider implements IOProvider {
    public static final Logger logger = Logger.getLogger(LocalIOProvider.class); 

    public void abort(IOHandle handle) throws IOException {
        ((Abortable) handle).abort();
    }

    public IOReader pull(String src, String dest, ReadIOCallback cb) throws IOException {
        return new Reader(src, cb);
    }

    public IOWriter push(String src, String dest, WriteIOCallback cb) throws IOException {
        return new Writer(dest, cb);
    }

    public boolean isDirect() {
        return true;
    }
    
    private static URI newURI(String src) throws IOException {
        try {
            return new URI(src);
        }
        catch (URISyntaxException e) {
            throw new IOException("Malformed URI: " + e.getMessage());
        }
    }

    private static class Writer implements IOWriter, WriteBufferCallback, Abortable {
        private File f;
        private long len, crt;
        private WriteIOCallback cb;
        private WriteBuffer buf;

        public Writer(String dest, WriteIOCallback cb) throws IOException {
            this.cb = cb;
            URI destURI = newURI(dest);
            f = CoasterFileRequestHandler.normalize(destURI.getPath().substring(1));
        }
        
        public String toString() {
            return "LW " + f;
        }

        public void close() throws IOException {
            buf.close();
        }

        public void setLength(long len) throws IOException {
            this.len = len;
            if (len == 0) {
                cb.done(this);
            }
            else {
                File p = f.getParentFile();
                if (!p.exists()) {
                    if (!p.mkdirs()) {
                        throw new IOException("Failed to create directory " + p.getAbsolutePath());
                    }
                }
                buf = Buffers.newWriteBuffer(new FileOutputStream(f).getChannel(), this);
            }
        }

        public void write(boolean last, byte[] data) throws IOException {
            try {
                buf.write(last, data);
                crt += data.length;
                if (last && crt != len) {
                    throw new IOException("File size mismatch. Expected " + len + " bytes, got "
                            + crt);
                }
            }
            catch (InterruptedException e) {
                throw new IOException(e.toString());
            }
        }

        public void done(boolean last) {
            if (last) {
                cb.done(this);
            }
        }

        public void error(boolean last, Exception e) {
            cb.error(this, e);
        }

        public void abort() throws IOException {
            buf.close();
            f.delete();
        }
    }

    private static class Reader implements IOReader, ReadBufferCallback {
        private File f;
        private ReadIOCallback cb;
        private ReadBuffer rbuf;
        private FileChannel fc;

        public Reader(String src, ReadIOCallback cb) throws IOException {
            URI srcURI = newURI(src);
            f = CoasterFileRequestHandler.normalize(srcURI.getPath().substring(1));
            this.cb = cb;
            fc = new FileInputStream(f).getChannel();
        }
        
        public String toString() {
            return "LR " + f;
        }

        public void start() throws IOException {
            cb.length(f.length());
            try {
                synchronized(this) {
                    rbuf = Buffers.newReadBuffer(fc, f.length(), this);
                }
            }
            catch (InterruptedException e) {
                throw new IOException(e.toString());
            }
        }

        public synchronized void dataSent() {
            rbuf.freeFirst();
        }

        public void dataRead(boolean last, ByteBuffer buf) {
            cb.data(this, buf, last);
            if (last) {
                close();
            }
        }

        private synchronized void closeBuffer() {
            try {
                rbuf.close();
            }
            catch (IOException e) {
                logger.warn("Failed to close read buffer", e);
            }
        }

        public void error(boolean last, Exception e) {
            cb.error(this, e);
            close();
        }

        public void close() {
            closeBuffer();
            try {
                fc.close();
            }
            catch (IOException e) {
                logger.warn("Failed to close file channel", e);
            }
        }

        public void abort() throws IOException {
            close();
        }
    }
}
