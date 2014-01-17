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
 * Implements a Speex decoder and RTP depacketizer using the native Speex
 * library.
 *
 * @author Lyubomir Marinov
 */
public class JNIDecoder
    extends AbstractCodec2
{

    /**
     * The list of <tt>Format</tt>s of audio data supported as input by
     * <tt>JNIDecoder</tt> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    /**
     * The list of <tt>Format</tt>s of audio data supported as output by
     * <tt>JNIDecoder</tt> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS
        = new Format[]
                {
                    new AudioFormat(
                            AudioFormat.LINEAR,
                            /* sampleRate */ Format.NOT_SPECIFIED,
                            16,
                            1,
                            AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                            AudioFormat.SIGNED,
                            /* frameSizeInBits */ Format.NOT_SPECIFIED,
                            /* frameRate */ Format.NOT_SPECIFIED,
                            Format.shortArray)
                };

    static
    {
        Speex.assertSpeexIsFunctional();

        double[] SUPPORTED_INPUT_SAMPLE_RATES
            = JNIEncoder.SUPPORTED_INPUT_SAMPLE_RATES;
        int supportedInputCount = SUPPORTED_INPUT_SAMPLE_RATES.length;

        SUPPORTED_INPUT_FORMATS = new Format[supportedInputCount];
        for (int i = 0; i < supportedInputCount; i++)
        {
            SUPPORTED_INPUT_FORMATS[i]
                = new AudioFormat(
                        Constants.SPEEX_RTP,
                        SUPPORTED_INPUT_SAMPLE_RATES[i],
                        /* sampleSizeInBits */ Format.NOT_SPECIFIED,
                        1,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        /* frameSizeInBits */ Format.NOT_SPECIFIED,
                        /* frameRate */ Format.NOT_SPECIFIED,
                        Format.byteArray);
        }
    }

    /**
     * The pointer to the native <tt>SpeexBits</tt> from which the native Speex
     * decoder (i.e. {@link #state}) reads the encoded audio data.
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
     * The sample rate configured into {@link #state}.
     */
    private int sampleRate = 0;

    /**
     * The native Speex decoder represented by this instance.
     */
    private long state = 0;

    /**
     * Initializes a new <tt>JNIDecoder</tt> instance.
     */
    public JNIDecoder()
    {
        super(
            "Speex JNI Decoder",
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
            Speex.decoder_destroy(state);
            state = 0;
            sampleRate = 0;
            frameSize = 0;
            duration = 0;
        }
        // bits
        Speex.bits_destroy(bits);
        bits = 0;
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
            throw new ResourceUnavailableException("speex_bits_init");
    }

    /**
     * Decodes Speex media from a specific input <tt>Buffer</tt>
     *
     * @param inBuffer input <tt>Buffer</tt>
     * @param outBuffer output <tt>Buffer</tt>
     * @return <tt>BUFFER_PROCESSED_OK</tt> if <tt>inBuffer</tt> has been
     * successfully processed
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
         * Make sure that the native Speex decoder which is represented by this
         * instance is configured to work with the inputFormat.
         */
        AudioFormat inAudioFormat = (AudioFormat) inFormat;
        int inSampleRate = (int) inAudioFormat.getSampleRate();

        if ((state != 0) && (sampleRate != inSampleRate))
        {
            Speex.decoder_destroy(state);
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
            state = Speex.decoder_init(mode);
            if (state == 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.decoder_ctl(state, Speex.SET_ENH, 1) != 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.decoder_ctl( state, Speex.SET_SAMPLING_RATE, inSampleRate)
                    != 0)
                return BUFFER_PROCESSED_FAILED;

            int frameSize = Speex.decoder_ctl(state, Speex.GET_FRAME_SIZE);

            if (frameSize < 0)
                return BUFFER_PROCESSED_FAILED;

            sampleRate = inSampleRate;
            this.frameSize = frameSize;
            duration = (frameSize * 1000 * 1000000) / sampleRate;
        }

        /* Read the encoded audio data from inputBuffer into the SpeexBits. */
        int inLength = inBuffer.getLength();

        if (inLength > 0)
        {
            byte[] in = (byte[]) inBuffer.getData();
            int inOffset = inBuffer.getOffset();

            Speex.bits_read_from(bits, in, inOffset, inLength);
            inLength = 0;
            inBuffer.setLength(inLength);
            inBuffer.setOffset(inOffset + inLength);
        }

        /* At long last, do the actual decoding. */
        int outLength = this.frameSize;
        boolean inputBufferNotConsumed;

        if (outLength > 0)
        {
            short[] out = validateShortArraySize(outBuffer, outLength, false);

            if (0 == Speex.decode_int(state, bits, out, 0))
            {
                outBuffer.setDuration(duration);
                outBuffer.setFormat(getOutputFormat());
                outBuffer.setLength(outLength);
                outBuffer.setOffset(0);
                inputBufferNotConsumed = (Speex.bits_remaining(bits) > 0);
            }
            else
            {
                outBuffer.setLength(0);
                discardOutputBuffer(outBuffer);
                inputBufferNotConsumed = false;
            }
        }
        else
        {
            outBuffer.setLength(0);
            discardOutputBuffer(outBuffer);
            inputBufferNotConsumed = false;
        }

        if ((inLength < 1) && !inputBufferNotConsumed)
            return BUFFER_PROCESSED_OK;
        else
            return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
    }

    /**
     * Get all supported output <tt>Format</tt>s.
     *
     * @param inputFormat input <tt>Format</tt> to determine corresponding output
     * <tt>Format/tt>s
     * @return array of supported <tt>Format</tt>
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
                                AudioFormat.LINEAR,
                                af.getSampleRate(),
                                16,
                                1,
                                AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                                AudioFormat.SIGNED,
                                /* frameSizeInBits */ Format.NOT_SPECIFIED,
                                /* frameRate */ Format.NOT_SPECIFIED,
                                Format.shortArray)
                    };
    }

    /**
     * Sets the <tt>Format</tt> of the media data to be input for processing in
     * this <tt>Codec</tt>.
     *
     * @param format the <tt>Format</tt> of the media data to be input for
     * processing in this <tt>Codec</tt>
     * @return the <tt>Format</tt> of the media data to be input for processing
     * in this <tt>Codec</tt> if <tt>format</tt> is compatible with this
     * <tt>Codec</tt>; otherwise, <tt>null</tt>
     * @see AbstractCodecExt#setInputFormat(Format)
     */
    @Override
    public Format setInputFormat(Format format)
    {
        Format inFormat = super.setInputFormat(format);

        if (inFormat != null)
        {
            double outSampleRate;
            int outChannels;

            if (outputFormat == null)
            {
                outSampleRate = Format.NOT_SPECIFIED;
                outChannels = Format.NOT_SPECIFIED;
            }
            else
            {
                AudioFormat outAudioFormat = (AudioFormat) outputFormat;

                outSampleRate = outAudioFormat.getSampleRate();
                outChannels = outAudioFormat.getChannels();
            }

            AudioFormat inAudioFormat = (AudioFormat) inFormat;
            double inSampleRate = inAudioFormat.getSampleRate();
            int inChannels = inAudioFormat.getChannels();

            if ((outSampleRate != inSampleRate) || (outChannels != inChannels))
            {
                setOutputFormat(
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                inSampleRate,
                                16,
                                inChannels,
                                AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                                AudioFormat.SIGNED,
                                /* frameSizeInBits */ Format.NOT_SPECIFIED,
                                /* frameRate */ Format.NOT_SPECIFIED,
                                Format.shortArray));
            }
        }
        return inFormat;
    }
}
