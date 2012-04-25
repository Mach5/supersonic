package de.rjan.subsonic.service;

import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.WeakHashMap;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class CachedTranscodingService extends TranscodingService {
	private static final Logger LOG = Logger.getLogger(CachedTranscodingService.class);
	private static final WeakHashMap<Parameters, GrowingBufferIOStream> transcodeCache = new WeakHashMap();

    // since the hash map doesn't really work - keep at least the last file...
    private static Parameters lastParameters = null;
    private static GrowingBufferIOStream lastTranscode = null;

	public InputStream getTranscodedInputStream(Parameters parameters) throws IOException {
        InputStream result;
        if (parameters.isDownsample() || parameters.isTranscode()) {
            GrowingBufferIOStream cachedBuffer = null; // = transcodeCache.get(parameters);
            if (parameters.equals(lastParameters)) {
                LOG.info("Found parameters " + ((lastTranscode == null)?"but lastTranscode null":"lastTranscode not null"));
                cachedBuffer = lastTranscode;
            } else {
                if (lastParameters == null) LOG.info("Last parameters was null");
                else if (!parameters.getMediaFile().equals(lastParameters.getMediaFile())) LOG.info("Mediafiles different old \"" + lastParameters.getMediaFile().getPath() + "\" new \"" + parameters.getMediaFile().getPath() + "\"");
                else LOG.info("OldParameters: "+lastParameters+" NewParameters:"+parameters);
            }
            
            if (cachedBuffer == null) {
                LOG.info("Didnt find parameters for file " + parameters.getMediaFile().getPath());
                InputStream transcodeStream = super.getTranscodedInputStream(parameters);
                cachedBuffer = new GrowingBufferIOStream(transcodeStream);
            } else {
                LOG.info("Found parameters for file " + parameters.getMediaFile().getPath());
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
        GrowingBufferIOStream cachedBuffer = null; //transcodeCache.get(parameters);
        if (parameters.equals(lastParameters)) cachedBuffer=lastTranscode;
        else LOG.info("getTranscodedLength parameters don't match last:"+lastParameters+" new:"+parameters);
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
