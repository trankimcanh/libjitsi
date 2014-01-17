/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.g722;

import javax.media.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.service.neomedia.codec.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class JNIDecoder
    extends AbstractCodec2
{
    static final Format[] SUPPORTED_INPUT_FORMATS
        = new Format[]
                {
                    new AudioFormat(
                            Constants.G722_RTP,
                            8000,
                            Format.NOT_SPECIFIED /* sampleSizeInBits */,
                            1)
                };

    static final Format[] SUPPORTED_OUTPUT_FORMATS
        = new Format[]
                {
                    new AudioFormat(
                            AudioFormat.LINEAR,
                            16000,
                            16,
                            1,
                            AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                            AudioFormat.SIGNED,
                            Format.NOT_SPECIFIED /* frameSizeInBits */,
                            Format.NOT_SPECIFIED /* frameRate */,
                            Format.shortArray)
                };

    static
    {
        System.loadLibrary("jng722");
    }

    private static native void g722_decoder_close(long decoder);

    private static native long g722_decoder_open();

    private static native void g722_decoder_process(
            long decoder,
            byte[] in, int inOffset,
            short[] out, int outOffset, int outLength);

    private long decoder;

    /**
     * Initializes a new <tt>JNIDecoder</tt> instance.
     */
    public JNIDecoder()
    {
        super("G.722 JNI Decoder", AudioFormat.class, SUPPORTED_OUTPUT_FORMATS);

        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    /**
     *
     * @see AbstractCodecExt#doClose()
     */
    @Override
    protected void doClose()
    {
        g722_decoder_close(decoder);
    }

    /**
     *
     * @throws ResourceUnavailableException
     * @see AbstractCodecExt#doOpen()
     */
    @Override
    protected void doOpen()
        throws ResourceUnavailableException
    {
        decoder = g722_decoder_open();
        if (decoder == 0)
            throw new ResourceUnavailableException("g722_decoder_open");
    }

    /**
     *
     * @param inBuffer
     * @param outBuffer
     * @return
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    @Override
    protected int doProcess(Buffer inBuffer, Buffer outBuffer)
    {
        byte[] in = (byte[]) inBuffer.getData();
        int outLength = inBuffer.getLength() * 2;
        short[] out = validateShortArraySize(outBuffer, outLength, false);

        g722_decoder_process(
                decoder,
                in, inBuffer.getOffset(),
                out, 0, outLength);

        outBuffer.setDuration(outLength * 1000000L / 16L /* kHz */);
        outBuffer.setFormat(getOutputFormat());
        outBuffer.setLength(outLength);

        return BUFFER_PROCESSED_OK;
    }
}
