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

/**
 *
 * @author Lyubomir Marinov
 */
public class JNIEncoder
    extends AbstractCodec2
{
    static
    {
        System.loadLibrary("jng722");
    }

    private static native void g722_encoder_close(long encoder);

    private static native long g722_encoder_open();

    private static native void g722_encoder_process(
            long encoder,
            short[] in, int inOffset,
            byte[] out, int outOffset, int outLength);

    private long encoder;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        super(
                "G.722 JNI Encoder",
                AudioFormat.class,
                JNIDecoder.SUPPORTED_INPUT_FORMATS);

        inputFormats = JNIDecoder.SUPPORTED_OUTPUT_FORMATS;
    }

    /**
     *
     * @param length
     * @return
     */
    private long computeDuration(long length)
    {
        return (length * 1000000L) / 8L;
    }

    /**
     *
     * @see AbstractCodecExt#doClose()
     */
    @Override
    protected void doClose()
    {
        g722_encoder_close(encoder);
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
        encoder = g722_encoder_open();
        if (encoder == 0)
            throw new ResourceUnavailableException("g722_encoder_open");
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
        short[] in = (short[]) inBuffer.getData();
        int outLength = inBuffer.getLength() / 2;
        byte[] out = validateByteArraySize(outBuffer, outLength, false);

        g722_encoder_process(
                encoder,
                in, inBuffer.getOffset(),
                out, 0, outLength);
        outBuffer.setDuration(computeDuration(outLength));
        outBuffer.setFormat(getOutputFormat());
        outBuffer.setLength(outLength);

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Get the output <tt>Format</tt>.
     *
     * @return output <tt>Format</tt> configured for this <tt>Codec</tt>
     * @see net.sf.fmj.media.AbstractCodec#getOutputFormat()
     */
    @Override
    public Format getOutputFormat()
    {
        Format f = super.getOutputFormat();

        if ((f != null) && (f.getClass() == AudioFormat.class))
        {
            AudioFormat af = (AudioFormat) f;

            setOutputFormat(
                    new AudioFormat(
                                af.getEncoding(),
                                af.getSampleRate(),
                                af.getSampleSizeInBits(),
                                af.getChannels(),
                                af.getEndian(),
                                af.getSigned(),
                                af.getFrameSizeInBits(),
                                af.getFrameRate(),
                                af.getDataType())
                            {
                                @Override
                                public long computeDuration(long length)
                                {
                                    return
                                        JNIEncoder.this.computeDuration(length);
                                }
                            });
        }
        return f;
    }
}
