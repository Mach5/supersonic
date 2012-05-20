package de.rjan.subsonic.service;

import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Collections;
import org.apache.commons.collections.map.LRUMap;

import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class CachedTranscodingService extends TranscodingService {
	private static final Logger LOG = Logger.getLogger(CachedTranscodingService.class);
	private static final Map transcodeCache = /* Collections.synchronizedMap( */ new LRUMap(10);

    // since the hash map doesn't really work - keep at least the last file...
    private static Parameters lastParameters = null;
    private static GrowingBufferIOStream lastTranscode = null;
   
	public InputStream getTranscodedInputStream(Parameters parameters) throws IOException {
        InputStream result;
        if (parameters.isDownsample() || parameters.isTranscode()) {
            GrowingBufferIOStream cachedBuffer = (GrowingBufferIOStream)transcodeCache.get(parameters);

            if (cachedBuffer == null) {
                InputStream transcodeStream = super.getTranscodedInputStream(parameters);
                cachedBuffer = new GrowingBufferIOStream(transcodeStream);
            }
            lastTranscode = cachedBuffer;
            lastParameters = parameters;
            transcodeCache.put(parameters, cachedBuffer);
            result = cachedBuffer.getInputStream();
        } else {
            result=super.getTranscodedInputStream(parameters);
        }
        return result;
	}

    public long getTranscodedLength(Parameters parameters) {
        GrowingBufferIOStream cachedBuffer = (GrowingBufferIOStream)transcodeCache.get(parameters);
        if (cachedBuffer == null) {
            LOG.info("No cached buffer found - "+parameters.getMediaFile());
            return 0;
        } else
            return cachedBuffer.getLength();
    }

    private class GrowingBufferIOStream {
        private InputStream transcodeStream;
        private ByteArrayOutputStream baout;
        private long length;

        public GrowingBufferIOStream(InputStream tcStream) {
            transcodeStream = tcStream;
            baout = null;
            length = 0;
        }

        private void createByteArray() throws IOException {
            baout = new ByteArrayOutputStream();
            length = baout.write(transcodeStream);
            transcodeStream.close();
            transcodeStream = null;
        }

        public synchronized InputStream getInputStream() throws IOException {
            if (baout == null) {
                createByteArray();
            }
            return new ByteArrayInputStream(baout.toByteArray());
        }

        public synchronized long getLength() {
            return length;
        }
    }

};
