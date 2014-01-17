/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.speex;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.sf.fmj.media.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;

/**
 * Implements an audio resampler using Speex.
 *
 * @author Lyubomir Marinov
 */
public class SpeexResampler
    extends AbstractCodec2
{
    /**
     * The list of <tt>Format</tt>s of audio data supported as input and output
     * by <tt>SpeexResampler</tt> instances.
     */
    private static final AudioFormat[] SUPPORTED_FORMATS;

    /**
     * The list of sample rates of audio data supported as input and output by
     * <tt>SpeexResampler</tt> instances.
     */
    private static final double[] SUPPORTED_SAMPLE_RATES
        = new double[]
                {
                    8000,
                    11025,
                    12000,
                    16000,
                    22050,
                    24000,
                    32000,
                    44100,
                    48000,
                    Format.NOT_SPECIFIED
                };

    static
    {
        Speex.assertSpeexIsFunctional();

        int supportedCount = SUPPORTED_SAMPLE_RATES.length;

        SUPPORTED_FORMATS = new AudioFormat[4 * supportedCount];
        for (int i = 0, j = 0; i < supportedCount; i++)
        {
            double sampleRate = SUPPORTED_SAMPLE_RATES[i];

            SUPPORTED_FORMATS[j++]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        sampleRate,
                        16 /* sampleSizeInBits */,
                        1 /* channels */,
                        AbstractAudioRenderer.NATIVE_AUDIO_FORMAT_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.byteArray);
            SUPPORTED_FORMATS[j++]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        sampleRate,
                        16 /* sampleSizeInBits */,
                        1 /* channels */,
                        AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.shortArray);
            SUPPORTED_FORMATS[j++]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        sampleRate,
                        16 /* sampleSizeInBits */,
                        2 /* channels */,
                        AbstractAudioRenderer.NATIVE_AUDIO_FORMAT_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.byteArray);
            SUPPORTED_FORMATS[j++]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        sampleRate,
                        16 /* sampleSizeInBits */,
                        2 /* channels */,
                        AbstractAudioRenderer.JAVA_AUDIO_FORMAT_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.shortArray);
        }
    }

    /**
     * The number of channels with which {@link #resampler} has been
     * initialized.
     */
    private int channels;

    /**
     * The input sample rate configured in {@link #resampler}.
     */
    private int inSampleRate;

    /**
     * The output sample rate configured in {@link #resampler}.
     */
    private int outSampleRate;

    /**
     * The pointer to the native <tt>SpeexResamplerState</tt> which is
     * represented by this instance.
     */
    private long resampler;

    /**
     * Initializes a new <tt>SpeexResampler</tt> instance.
     */
    public SpeexResampler()
    {
        super("Speex Resampler", AudioFormat.class, SUPPORTED_FORMATS);

        inputFormats = SUPPORTED_FORMATS;
    }

    /**
     * @see AbstractCodecExt#doClose()
     */
    @Override
    protected void doClose()
    {
        if (resampler != 0)
        {
            Speex.resampler_destroy(resampler);
            resampler = 0;
        }
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
    }

    /**
     * Resamples audio from a specific input <tt>Buffer</tt> into a specific
     * output <tt>Buffer</tt>.
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

        AudioFormat inAudioFormat = (AudioFormat) inFormat;
        int inSampleRate = (int) inAudioFormat.getSampleRate();
        AudioFormat outAudioFormat = (AudioFormat) getOutputFormat();
        int outSampleRate = (int) outAudioFormat.getSampleRate();
        int ret;

        if (inSampleRate == outSampleRate)
        {
            ret
                = passThrough(
                        inBuffer, inAudioFormat,
                        outBuffer, outAudioFormat);
        }
        else
        {
            ret
                = resample(
                        inBuffer, inAudioFormat, inSampleRate,
                        outBuffer, outAudioFormat, outSampleRate);
        }
        if (ret != BUFFER_PROCESSED_FAILED)
        {
            outBuffer.setDuration(inBuffer.getDuration());
            outBuffer.setEOM(inBuffer.isEOM());
            outBuffer.setFlags(inBuffer.getFlags());
            outBuffer.setHeader(inBuffer.getHeader());
            outBuffer.setSequenceNumber(inBuffer.getSequenceNumber());
            outBuffer.setTimeStamp(inBuffer.getTimeStamp());
        }

        return ret;
    }

    /**
     * Get the output formats matching a specific input format.
     *
     * @param inFormat the input format to get the matching output formats of
     * @return the output formats matching the specified <tt>inFormat</tt>
     * @see AbstractCodecExt#getMatchingOutputFormats(Format)
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inFormat)
    {
        if (inFormat instanceof AudioFormat)
        {
            AudioFormat inAudioFormat = (AudioFormat) inFormat;
            int inChannels = inAudioFormat.getChannels();
            List<Format> matchingOutFormats = new ArrayList<Format>();

            for (AudioFormat supportedFormat : SUPPORTED_FORMATS)
            {
                if (supportedFormat.getChannels() == inChannels)
                    matchingOutFormats.add(supportedFormat);
            }
            return
                matchingOutFormats.toArray(
                        new Format[matchingOutFormats.size()]);
        }
        else
            return null;
    }

    private int passThrough(
            Buffer inBuffer, AudioFormat inFormat,
            Buffer outBuffer, AudioFormat outFormat)
    {
        Class<?> inDataType = inFormat.getDataType();
        int inLength = inBuffer.getLength();
        int inOffset = inBuffer.getOffset();
        Class<?> outDataType = outFormat.getDataType();

        if (Format.byteArray.equals(inDataType))
        {
            byte[] in = (byte[]) inBuffer.getData();

            if (Format.byteArray.equals(outDataType))
            {
                byte[] out = validateByteArraySize(outBuffer, inLength, false);

                if ((in != null) && (out != null))
                    System.arraycopy(in, inOffset, out, 0, inLength);
                outBuffer.setFormat(inBuffer.getFormat());
                outBuffer.setLength(inLength);
            }
            else
            {
                int outLength = inLength / 2;
                short[] out
                    = validateShortArraySize(outBuffer, outLength, false);

                if (outFormat.getEndian() == AudioFormat.LITTLE_ENDIAN)
                {
                    for (int i = inOffset, o = 0; o < outLength; o++)
                        out[o] = (short) ((in[i++] << 8) | (in[i++] & 0xFF));
                }
                else
                {
                    for (int i = inOffset, o = 0; o < outLength; i += 2, o++)
                        out[o] = ArrayIOUtils.readShort(in, i);
                }
                outBuffer.setFormat(outFormat);
                outBuffer.setLength(outLength);
            }
        }
        else
        {
            short[] in = (short[]) inBuffer.getData();

            if (Format.byteArray.equals(outDataType))
            {
                int outLength = inLength * 2;
                byte[] out = validateByteArraySize(outBuffer, outLength, false);

                if (outFormat.getEndian() == AudioFormat.BIG_ENDIAN)
                {
                    for (int i = inOffset, o = 0; o < outLength; i++)
                    {
                        short s = in[i];

                        out[o++] = (byte) (s >> 8);
                        out[o++] = (byte) (s & 0xFF);
                    }
                }
                else
                {
                    for (int i = inOffset, o = 0; o < outLength; i++, o += 2)
                        ArrayIOUtils.writeShort(in[i], out, o);
                }
                outBuffer.setFormat(outFormat);
                outBuffer.setLength(outLength);
            }
            else
            {
                short[] out
                    = validateShortArraySize(outBuffer, inLength, false);

                if ((in != null) && (out != null))
                    System.arraycopy(in, inOffset, out, 0, inLength);
                outBuffer.setFormat(inBuffer.getFormat());
                outBuffer.setLength(inLength);
            }
        }
        outBuffer.setOffset(0);

        return BUFFER_PROCESSED_OK;
    }

    private int resample(
            Buffer inBuffer, AudioFormat inAudioFormat, int inSampleRate,
            Buffer outBuffer, AudioFormat outAudioFormat, int outSampleRate)
    {
        int channels = inAudioFormat.getChannels();

        if (outAudioFormat.getChannels() != channels)
            return BUFFER_PROCESSED_FAILED;

        boolean channelsHaveChanged = (this.channels != channels);

        if (channelsHaveChanged
                || (this.inSampleRate != inSampleRate)
                || (this.outSampleRate != outSampleRate))
        {
            if (channelsHaveChanged && (resampler != 0))
            {
                Speex.resampler_destroy(resampler);
                resampler = 0;
            }
            if (resampler == 0)
            {
                resampler
                    = Speex.resampler_init(
                            channels,
                            inSampleRate,
                            outSampleRate,
                            Speex.RESAMPLER_QUALITY_VOIP,
                            0);
            }
            else
            {
                Speex.resampler_set_rate(
                        resampler,
                        inSampleRate,
                        outSampleRate);
            }
            if (resampler != 0)
            {
                this.inSampleRate = inSampleRate;
                this.outSampleRate = outSampleRate;
                this.channels = channels;
            }
        }
        if (resampler == 0)
            return BUFFER_PROCESSED_FAILED;

        int inLength = inBuffer.getLength();
        int inOffset = inBuffer.getOffset();

        if (Format.shortArray.equals(inAudioFormat.getDataType()))
        {
            inLength *= 2;
            inOffset *= 2;
        }

        int frameSize = channels * (inAudioFormat.getSampleSizeInBits() / 8);
        /*
         * XXX The numbers of input and output samples which are to be specified
         * to the function resampler_process_interleaved_int are per-channel.
         */
        int inSampleCount = inLength / frameSize;
        int outSampleCount = inSampleCount * outSampleRate / inSampleRate;
        int outLength;
        Object out;

        if (Format.byteArray.equals(outAudioFormat.getDataType()))
        {
            outLength = outSampleCount * frameSize;
            out = validateByteArraySize(outBuffer, outLength, false);
        }
        else
        {
            outLength = outSampleCount * frameSize / 2;
            out = validateShortArraySize(outBuffer, outLength, false);
        }

        /*
         * XXX The method Speex.resampler_process_interleaved_int will crash if
         * in is null.
         */
        if (inSampleCount == 0)
        {
            outSampleCount = 0;
        }
        else
        {
            Object in = inBuffer.getData();

            outSampleCount
                = Speex.resampler_process_interleaved_int(
                        resampler,
                        in, inOffset, inSampleCount,
                        out, 0, outSampleCount);
        }
        outBuffer.setFormat(outAudioFormat);
        outBuffer.setLength(outLength);
        outBuffer.setOffset(0);

        return BUFFER_PROCESSED_OK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Format setInputFormat(Format format)
    {
        AudioFormat inFormat = (AudioFormat) super.setInputFormat(format);

        if (inFormat != null)
        {
            AudioFormat outFormat = (AudioFormat) this.outputFormat;
            double outSampleRate;
            int outEndian;
            Class<?> outDataType;

            if (outFormat == null)
            {
                outSampleRate = inFormat.getSampleRate();
                outEndian = inFormat.getEndian();
                outDataType = inFormat.getDataType();
            }
            else
            {
                outSampleRate = outFormat.getSampleRate();
                if (outSampleRate == Format.NOT_SPECIFIED)
                    outSampleRate = inFormat.getSampleRate();
                outEndian = outFormat.getEndian();
                if (outEndian == Format.NOT_SPECIFIED)
                    outEndian = inFormat.getEndian();
                outDataType = outFormat.getDataType();
                if (outDataType == null)
                    outDataType = inFormat.getDataType();
            }

            setOutputFormat(
                    new AudioFormat(
                            inFormat.getEncoding(),
                            outSampleRate,
                            inFormat.getSampleSizeInBits(),
                            inFormat.getChannels(),
                            outEndian,
                            inFormat.getSigned(),
                            /* frameSizeInBits */ Format.NOT_SPECIFIED,
                            /* frameRate */ Format.NOT_SPECIFIED,
                            outDataType));
        }

        return inFormat;
    }
}
