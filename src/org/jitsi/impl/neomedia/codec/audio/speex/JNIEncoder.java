/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.speex;

import javax.media.*;
import javax.media.format.*;

import net.sf.fmj.media.*;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Implements a Speex encoder and RTP packetizer using the native Speex library.
 *
 * @author Lyubomir Marinov
 */
public class JNIEncoder
    extends AbstractCodec2
{

    /**
     * The list of <tt>Format</tt>s of audio data supported as input by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    /**
     * The list of sample rates of audio data supported as input by
     * <tt>JNIEncoder</tt> instances.
     */
    static final double[] SUPPORTED_INPUT_SAMPLE_RATES
        = new double[] { 8000, 16000, 32000 };

    /**
     * The list of <tt>Format</tt>s of audio data supported as output by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS
        = new Format[] { new AudioFormat(Constants.SPEEX_RTP) };

    static
    {
        Speex.assertSpeexIsFunctional();

        int supportedInputCount = SUPPORTED_INPUT_SAMPLE_RATES.length;

        SUPPORTED_INPUT_FORMATS = new Format[supportedInputCount];
        for (int i = 0; i < supportedInputCount; i++)
        {
            SUPPORTED_INPUT_FORMATS[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_INPUT_SAMPLE_RATES[i],
                        16,
                        1,
                        AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                        AudioFormat.SIGNED,
                        /* frameSizeInBits */ Format.NOT_SPECIFIED,
                        /* frameRate */ Format.NOT_SPECIFIED,
                        Format.shortArray);
        }
    }

    /**
     * The pointer to the native <tt>SpeexBits</tt> into which the native Speex
     * encoder (i.e. {@link #state}) writes the encoded audio data.
     */
    private long bits = 0;

    /**
     * The duration in nanoseconds of an output <tt>Buffer</tt> produced by this
     * <tt>Codec</tt>.
     */
    private long duration = 0;

    /**
     * The number of bytes from an input <tt>Buffer</tt> that this
     * <tt>Codec</tt> processes in one call of its
     * {@link #process(Buffer, Buffer)}.
     */
    private int frameSize = 0;

    /**
     * The bytes from an input <tt>Buffer</tt> from a previous call to
     * {@link #process(Buffer, Buffer)} that this <tt>Codec</tt> didn't process
     * because the total number of bytes was less than {@link #frameSize} and
     * need to be prepended to a subsequent input <tt>Buffer</tt> in order to
     * process a total of {@link #frameSize} bytes.
     */
    private short[] prevIn;

    /**
     * The length of the audio data in {@link #prevIn}.
     */
    private int prevInLength = 0;

    /**
     * The sample rate configured into {@link #state}.
     */
    private int sampleRate = 0;

    /**
     * The native Speex encoder represented by this instance.
     */
    private long state = 0;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        super(
            "Speex JNI Encoder",
            AudioFormat.class,
            SUPPORTED_OUTPUT_FORMATS);

        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    /**
     * @see AbstractCodecExt#doClose()
     */
    @Override
    protected void doClose()
    {
        // state
        if (state != 0)
        {
            Speex.encoder_destroy(state);
            state = 0;
            sampleRate = 0;
            frameSize = 0;
            duration = 0;
        }
        // bits
        Speex.bits_destroy(bits);
        bits = 0;
        // previousInput
        prevIn = null;
    }

    /**
     * Opens this <tt>Codec</tt> and acquires the resources that it needs to
     * operate. A call to {@link PlugIn#open()} on this instance will result in
     * a call to <tt>doOpen</tt> only if {@link AbstractCodec#opened} is
     * <tt>false</tt>. All required input and/or output formats are assumed to
     * have been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
     *
     * @throws ResourceUnavailableException if any of the resources that this
     * <tt>Codec</tt> needs to operate cannot be acquired
     * @see AbstractCodecExt#doOpen()
     */
    @Override
    protected void doOpen()
        throws ResourceUnavailableException
    {
        bits = Speex.bits_init();
        if (bits == 0)
            throw new ResourceUnavailableException("bits_init");
    }

    /**
     * Processes (encode) a specific input <tt>Buffer</tt>.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    @Override
    protected int doProcess(Buffer inBuffer, Buffer outBuffer)
    {
        Format inFormat = inBuffer.getFormat();

        if ((inFormat != null)
                && (inFormat != this.inputFormat)
                && !inFormat.equals(this.inputFormat))
        {
            if (null == setInputFormat(inFormat))
                return BUFFER_PROCESSED_FAILED;
        }
        inFormat = this.inputFormat;

        /*
         * Make sure that the native Speex encoder which is represented by this
         * instance is configured to work with the inputFormat.
         */
        AudioFormat inAudioFormat = (AudioFormat) inFormat;
        int inSampleRate = (int) inAudioFormat.getSampleRate();

        if ((state != 0) && (sampleRate != inSampleRate))
        {
            Speex.encoder_destroy(state);
            state = 0;
            sampleRate = 0;
            frameSize = 0;
        }
        if (state == 0)
        {
            long mode
                = Speex.lib_get_mode(
                        (inSampleRate == 16000)
                            ? Speex.MODEID_WB
                            : (inSampleRate == 32000)
                                ? Speex.MODEID_UWB
                                : Speex.MODEID_NB);

            if (mode == 0)
                return BUFFER_PROCESSED_FAILED;
            state = Speex.encoder_init(mode);
            if (state == 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.encoder_ctl(state, Speex.SET_QUALITY, 4) != 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.encoder_ctl(state, Speex.SET_SAMPLING_RATE, inSampleRate)
                    != 0)
                return BUFFER_PROCESSED_FAILED;

            int frameSize = Speex.encoder_ctl(state, Speex.GET_FRAME_SIZE);

            if (frameSize < 0)
                return BUFFER_PROCESSED_FAILED;

            sampleRate = inSampleRate;
            this.frameSize = frameSize;
            duration = (((long) frameSize) * 1000 * 1000000) / (sampleRate);
        }

        /*
         * The native Speex encoder always processes frameSize bytes from the
         * input in one call. If any specified inputBuffer is with a different
         * length, then we'll have to wait for more bytes to arrive until we
         * have frameSize bytes. Remember whatever is left unprocessed in
         * previousInput and prepend it to the next inputBuffer.
         */
        short[] in = (short[]) inBuffer.getData();
        int inLength = inBuffer.getLength();
        int inOffset = inBuffer.getOffset();

        if ((prevIn != null) && (prevInLength > 0))
        {
            if (prevInLength < this.frameSize)
            {
                if (prevIn.length < this.frameSize)
                {
                    short[] newPrevIn = new short[this.frameSize];

                    System.arraycopy(prevIn, 0, newPrevIn, 0, prevIn.length);
                    prevIn = newPrevIn;
                }

                int shortsToCopyFromInToPrevIn
                    = Math.min(this.frameSize - prevInLength, inLength);

                if (shortsToCopyFromInToPrevIn > 0)
                {
                    System.arraycopy(
                            in, inOffset,
                            prevIn, prevInLength,
                            shortsToCopyFromInToPrevIn);
                    prevInLength += shortsToCopyFromInToPrevIn;
                    inLength -= shortsToCopyFromInToPrevIn;
                    inBuffer.setLength(inLength);
                    inBuffer.setOffset(inOffset + shortsToCopyFromInToPrevIn);
                }
            }

            if (prevInLength == this.frameSize)
            {
                in = prevIn;
                inOffset = 0;
                prevInLength = 0;
            }
            else if (prevInLength > this.frameSize)
            {
                in = new short[this.frameSize];
                System.arraycopy(prevIn, 0, in, 0, in.length);
                inOffset = 0;
                prevInLength -= in.length;
                System.arraycopy(prevIn, in.length, prevIn, 0, prevInLength);
            }
            else
            {
                outBuffer.setLength(0);
                discardOutputBuffer(outBuffer);
                if (inLength < 1)
                    return BUFFER_PROCESSED_OK;
                else
                    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            }
        }
        else if (inLength < 1)
        {
            outBuffer.setLength(0);
            discardOutputBuffer(outBuffer);
            return BUFFER_PROCESSED_OK;
        }
        else if (inLength < this.frameSize)
        {
            if ((prevIn == null) || (prevIn.length < inLength))
                prevIn = new short[this.frameSize];
            System.arraycopy(in, inOffset, prevIn, 0, inLength);
            prevInLength = inLength;
            outBuffer.setLength(0);
            discardOutputBuffer(outBuffer);
            return BUFFER_PROCESSED_OK;
        }
        else
        {
            inLength -= this.frameSize;
            inBuffer.setLength(inLength);
            inBuffer.setOffset(inOffset + this.frameSize);
        }

        /* At long last, do the actual encoding. */
        Speex.bits_reset(bits);
        Speex.encode_int(state, in, inOffset, bits);

        /* Read the encoded audio data from the SpeexBits into outputBuffer. */
        int outLength = Speex.bits_nbytes(bits);

        if (outLength > 0)
        {
            byte[] out = validateByteArraySize(outBuffer, outLength, false);

            outLength = Speex.bits_write(bits, out, 0, out.length);
            if (outLength > 0)
            {
                outBuffer.setDuration(duration);
                outBuffer.setFormat(getOutputFormat());
                outBuffer.setLength(outLength);
                outBuffer.setOffset(0);
            }
            else
            {
                outBuffer.setLength(0);
                discardOutputBuffer(outBuffer);
            }
        }
        else
        {
            outBuffer.setLength(0);
            discardOutputBuffer(outBuffer);
        }

        if (inLength < 1)
            return BUFFER_PROCESSED_OK;
        else
            return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
    }

    /**
     * Get the output formats matching a specific input format.
     *
     * @param inputFormat the input format to get the matching output formats of
     * @return the output formats matching the specified input format
     * @see AbstractCodecExt#getMatchingOutputFormats(Format)
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        AudioFormat af = (AudioFormat) inputFormat;

        return
            new Format[]
                    {
                        new AudioFormat(
                                Constants.SPEEX_RTP,
                                af.getSampleRate(),
                                /* sampleSizeInBits */ Format.NOT_SPECIFIED,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                /* frameSizeInBits */ Format.NOT_SPECIFIED,
                                /* frameRate */ Format.NOT_SPECIFIED,
                                Format.byteArray)
                    };
    }

    /**
     * Gets the output format.
     *
     * @return output format
     * @see net.sf.fmj.media.AbstractCodec#getOutputFormat()
     */
    @Override
    public Format getOutputFormat()
    {
        Format f = super.getOutputFormat();

        if ((f != null) && (f.getClass() == AudioFormat.class))
        {
            AudioFormat af = (AudioFormat) f;

            f
                = setOutputFormat(
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
                                        return JNIEncoder.this.duration;
                                    }
                                });
        }
        return f;
    }

    /**
     * Sets the input format.
     *
     * @param format format to set
     * @return format
     * @see AbstractCodecExt#setInputFormat(Format)
     */
    @Override
    public Format setInputFormat(Format format)
    {
        Format inputFormat = super.setInputFormat(format);

        if (inputFormat != null)
        {
            double outputSampleRate;
            int outputChannels;

            if (outputFormat == null)
            {
                outputSampleRate = Format.NOT_SPECIFIED;
                outputChannels = Format.NOT_SPECIFIED;
            }
            else
            {
                AudioFormat outputAudioFormat = (AudioFormat) outputFormat;

                outputSampleRate = outputAudioFormat.getSampleRate();
                outputChannels = outputAudioFormat.getChannels();
            }

            AudioFormat inputAudioFormat = (AudioFormat) inputFormat;
            double inputSampleRate = inputAudioFormat.getSampleRate();
            int inputChannels = inputAudioFormat.getChannels();

            if ((outputSampleRate != inputSampleRate)
                    || (outputChannels != inputChannels))
            {
                setOutputFormat(
                    new AudioFormat(
                            Constants.SPEEX_RTP,
                            inputSampleRate,
                            Format.NOT_SPECIFIED,
                            inputChannels,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.byteArray));
            }
        }
        return inputFormat;
    }
}
