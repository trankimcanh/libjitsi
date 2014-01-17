/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.speex;

/**
 * Provides the interface to the native Speex library.
 *
 * @author Lyubomir Marinov
 */
public final class Speex
{
    public static final int GET_FRAME_SIZE = 3;

    public static final int MODEID_NB = 0;

    public static final int MODEID_UWB = 2;

    public static final int MODEID_WB = 1;

    public static final int RESAMPLER_QUALITY_VOIP = 3;

    public static final int SET_ENH = 0;

    public static final int SET_QUALITY = 4;

    public static final int SET_SAMPLING_RATE = 24;

    static
    {
        System.loadLibrary("jnspeex");
    }

    public static void assertSpeexIsFunctional()
    {
        lib_get_mode(MODEID_NB);
    }

    public static native void bits_destroy(long bits);

    public static native long bits_init();

    public static native int bits_nbytes(long bits);

    public static native void bits_read_from(
            long bits,
            byte[] bytes, int bytesOffset,
            int len);

    public static native int bits_remaining(long bits);

    public static native void bits_reset(long bits);

    public static native int bits_write(
            long bits,
            byte[] bytes, int bytesOffset,
            int max_len);

    public static native int decode_int(
            long state,
            long bits,
            short[] out, int outOffset);

    public static native int decoder_ctl(long state, int request);

    public static native int decoder_ctl(long state, int request, int value);

    public static native void decoder_destroy(long state);

    public static native long decoder_init(long mode);

    public static native int encode_int(
            long state,
            short[] in, int inOffset,
            long bits);

    public static native int encoder_ctl(long state, int request);

    public static native int encoder_ctl(long state, int request, int value);

    public static native void encoder_destroy(long state);

    public static native long encoder_init(long mode);

    public static native long lib_get_mode(int mode);

    public static native void resampler_destroy(long state);

    public static native long resampler_init(
            int nb_channels,
            int in_rate,
            int out_rate,
            int quality,
            long err);

    /**
     * Resamples from an interleaved primitive-type input array into a
     * primitive-type output array. The input and output buffers must <b>not</b>
     * overlap. Speex works with 16-bit signed samples only and, consequently,
     * the primitive types supported by the method are <tt>byte</tt> and
     * <tt>short</tt>. 
     *
     * @param state the <tt>SpeexResamplerState</tt> to perform the resamping
     * @param in a primitive-type array which represents the input samples to be
     * resampled
     * @param inOffsetInBytes the offset in <tt>in</tt> expressed in
     * <b>bytes</b> at which the input samples to be resampled start 
     * @param inSampleCount the number of input samples per channel in
     * <tt>in</tt> starting at <tt>inOffsetInBytes</tt> to be resampled
     * @param out a primitive-type array which represents the output buffer in
     * which the resampled samples are to be written
     * @param outOffsetInBytes the offset in <tt>out</tt> expressed in
     * <b>bytes</b> at which the writing of the output samples is to start
     * @param outSampleCount the number of output samples per channel allocated
     * in <tt>out</tt> starting at <tt>outOffsetInBytes</tt>
     * @return the number of output samples per channel written into
     * <tt>out</tt> starting at <tt>outOffsetInBytes</tt>
     */
    public static native int resampler_process_interleaved_int(
            long state,
            Object in, int inOffsetInBytes, int inSampleCount,
            Object out, int outOffsetInBytes, int outSampleCount);

    public static native int resampler_set_rate(
            long state,
            int in_rate,
            int out_rate);

    /** Prevents the creation of <tt>Speex</tt> instances. */
    private Speex() {}
}
