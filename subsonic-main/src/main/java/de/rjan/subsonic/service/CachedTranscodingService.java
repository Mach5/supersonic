package de.rjan.subsonic.service;

import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.WeakHashMap;

public class CachedTranscodingService extends TranscodingService {
	private static final Logger LOG = Logger.getLogger(CachedTranscodingService.class);
	private static final WeakHashMap<Parameters, byte[]> transcodeCache = new WeakHashMap();

    // since the hash map doesn't really work - keep at least the last file...
    private Parameters lastParameters;
    private byte[] lastTranscode;

	public InputStream getTranscodedInputStream(Parameters parameters, TransferStatus status) throws IOException {
        InputStream result;
        if (parameters.isDownsample() || parameters.isTranscode()) {
            byte[] cachedBuffer = null; // = transcodeCache.get(parameters);
            if (parameters.equals(lastParameters)) {
                cachedBuffer = lastTranscode;
            } else {
                if (lastParameters == null) LOG.info("Last parameters was null");
                else if (!parameters.getMediaFile().equals(lastParameters.getMediaFile())) LOG.info("Mediafiles different old \"" + lastParameters.getMediaFile().getPath() + "\" new \"" + parameters.getMediaFile().getPath() + "\"");
            }
            
            if (cachedBuffer == null) {
                LOG.info("Didnt find parameters for file " + parameters.getMediaFile().getPath());
                InputStream transcodeStream = super.getTranscodedInputStream(parameters);
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                byte[] buf = new byte[2048];
                int n = 0;
                n = transcodeStream.read(buf);
                while (n>=0) {
                    if (status.terminated()) {
                        return null;
                    }     

                    baout.write(buf,0,n);
                    n = transcodeStream.read(buf);
                } 
                cachedBuffer = baout.toByteArray();
                lastTranscode = cachedBuffer;
            } else {
                LOG.info("Found parameters for file " + parameters.getMediaFile().getPath());
            }
            lastParameters = parameters;
            transcodeCache.put(parameters, cachedBuffer);
            result = new ByteArrayInputStream(cachedBuffer);
        } else {
            result=super.getTranscodedInputStream(parameters);
        }
        return result;
	}

    public long getTranscodedLength(Parameters parameters) {
        byte[] cachedBuffer = null; //transcodeCache.get(parameters);
        if (parameters.equals(lastParameters)) cachedBuffer=lastTranscode;
        if (cachedBuffer == null)
            return 0;
        else
            return cachedBuffer.length;
    }

};
