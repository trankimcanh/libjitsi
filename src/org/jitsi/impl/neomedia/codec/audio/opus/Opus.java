/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.opus;

/**
 * Defines the API of the native opus library to be utilized by the libjitsi
 * library.
 *
 * @author Boris Grozev
 * @author Lyubomir Marinov
 */
public class Opus
{
    /**
     * Constant used to set various settings to "automatic".
     */
    public static final int AUTO = -1000;

    /**
     * Opus fullband constant
     */
    public static final int BANDWIDTH_FULLBAND = 1105;

    /**
     * Opus mediumband constant
     */
    public static final int BANDWIDTH_MEDIUMBAND = 1102;

    /**
     * Opus narrowband constant
     */
    public static final int BANDWIDTH_NARROWBAND = 1101;

    /**
     * Opus superwideband constant
     */
    public static final int BANDWIDTH_SUPERWIDEBAND = 1104;

    /**
     * Opus wideband constant
     */
    public static final int BANDWIDTH_WIDEBAND = 1103;

    /**
     * Opus constant for an invalid packet
     */
    public static final int INVALID_PACKET = -4;

    /**
     * The maximum size of a packet we can create. Since we're only creating
     * packets with a single frame, that's a 1 byte TOC + the maximum frame size.
     * See http://tools.ietf.org/html/rfc6716#section-3.2
     */
    public static final int MAX_PACKET = 1 + 1275;

    /**
     * Constant usually indicating that no error occurred.
     */
    public static final int OK = 0;

    /**
     * Loads the native JNI library.
     */
    static
    {
        System.loadLibrary("jnopus");
    }

    /**
     * Asserts that the <tt>Opus</tt> class and the JNI library which supports
     * it are functional. The method is to be invoked early (e.g. static/class
     * initializers) by classes which require it (i.e. they depend on it and
     * they cannot function without it).
     */
    public static void assertOpusIsFunctional()
    {
        decoder_get_size(1);
        encoder_get_size(1);
    }

    /**
     * Decodes an Opus packet from <tt>in</tt> into <tt>out</tt>.
     *
     * @param decoder the <tt>OpusDecoder</tt> state to perform the decoding
     * @param in an array of <tt>byte</tt>s which represents the input payload
     * to decode. If <tt>null</tt>, indicates packet loss.
     * @param inOffset the offset in <tt>in</tt> at which the payload to be
     * decoded begins
     * @param inLength the length in bytes in <tt>in</tt> beginning at
     * <tt>inOffset</tt> of the payload to be decoded
     * @param out an array of <tt>short</tt>s into which the decoded signal is
     * to be output
     * @param outOffset the offset in <tt>out</tt> at which the output of the
     * decoded signal is to begin
     * @param outFrameSize the number of samples per channel <tt>out</tt>
     * beginning at <tt>outOffset</tt> of the maximum space available for output
     * of the decoded signal
     * @param decodeFEC <tt>0</tt> to decode the packet normally, <tt>1</tt> to
     * decode the FEC data in the packet
     * @return the number of decoded samples written into <tt>out</tt>
     * (beginning at <tt>outOffset</tt>)
     */
    public static native int decode(
            long decoder,
            byte[] in, int inOffset, int inLength,
            short[] out, int outOffset, int outFrameSize,
            int decodeFEC);

    /**
     * Creates an OpusDecoder structure, returns a pointer to it or 0 on error.
     *
     * @param Fs Sample rate to decode to
     * @param channels number of channels to decode to(1/2)
     *
     * @return A pointer to the OpusDecoder structure created, 0 on error.
     */
    public static native long decoder_create(int Fs, int channels);

    /**
     * Destroys an OpusDecoder, freeing it's resources.
     *
     * @param decoder Address of the structure (as returned from decoder_create)
     */
    public static native void decoder_destroy(long decoder);

    /**
     * Returns the number of samples in an opus packet
     *
     * @param decoder The decoder to use.
     * @param packet Array holding the packet.
     * @param offset Offset into packet where the actual packet begins.
     * @param length Length of the packet.
     *
     * @return the number of samples in <tt>packet</tt> .
     */
    public static native int decoder_get_nb_samples(
            long decoder,
            byte[] packet, int offset, int length);

    /**
     * Returns the size in bytes required for an OpusDecoder structure.
     *
     * @param channels number of channels (1/2)
     *
     * @return the size in bytes required for an OpusDecoder structure.
     */
    public static native int decoder_get_size(int channels);

    /**
     * Encodes the input from <tt>in</tt> into an Opus packet in <tt>out</tt>.
     *
     * @param encoder The encoder to use.
     * @param in Array containing PCM encoded input.
     * @param inOffset Offset to use into the <tt>in</tt> array
     * @param inFrameSize The number of samples per channel in <tt>in</tt>.
     * @param out Array where the encoded packet will be stored.
     * @param outOffset
     * @param outLength The number of available bytes in <tt>out</tt>.
     *
     * @return The number of bytes written in <tt>out</tt>, or a negative on
     * error.
     */
    public static native int encode(
            long encoder,
            short[] in, int inOffset, int inFrameSize,
            byte[] out, int outOffset, int outLength);

    /**
     * Creates an OpusEncoder structure, returns a pointer to it casted to long.
     * The native function's <tt>application</tt> parameter is always set to
     * OPUS_APPLICATION_VOIP.
     *
     * @param Fs Sample rate of the input PCM
     * @param channels number of channels in the input (1/2)
     *
     * @return A pointer to the OpusEncoder structure created, 0 on error
     */
    public static native long encoder_create(int Fs, int channels);

    /**
     * Destroys an OpusEncoder, freeing it's resources.
     *
     * @param encoder Address of the structure (as returned from encoder_create)
     */
    public static native void encoder_destroy(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns the
     * current encoder audio bandwidth
     * .
     * @param encoder The encoder to use
     *
     * @return the current encoder audio bandwidth
     */
    public static native int encoder_get_bandwidth(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns the
     * current encoder bitrate.
     *
     * @param encoder The encoder to use
     *
     * @return The current encoder bitrate.
     */
    public static native int encoder_get_bitrate(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns
     * the current DTX setting of the encoder.
     *
     * @param encoder The encoder to use
     *
     * @return the current DTX setting of the encoder.
     */
    public static native int encoder_get_dtx(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns
     * the current inband FEC encoder setting.
     *
     * @param encoder The encoder to use
     *
     * @return the current inband FEC encoder setting.
     */
    public static native int encoder_get_inband_fec(long encoder);

    /**
     * Returns the size in bytes required for an OpusEncoder structure.
     *
     * @param channels number of channels (1/2)
     *
     * @return the size in bytes required for an OpusEncoder structure.
     */
    public static native int encoder_get_size(int channels);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns the
     * current encoder VBR setting
     *
     * @param encoder The encoder to use
     *
     * @return The current encoder VBR setting.
     */
    public static native int encoder_get_vbr(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Returns
     * the current VBR constraint encoder setting.
     *
     * @param encoder The encoder to use
     *
     * @return the current VBR constraint encoder setting.
     */
    public static native int encoder_get_vbr_constraint(long encoder);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder audio bandwidth.
     *
     * @param encoder The encoder to use
     * @param bandwidth The bandwidth to set, should be one of
     *  <tt>BANDWIDTH_FULLBAND</tt>, <tt>BANDWIDTH_MEDIUMBAND</tt>,
     *  <tt>BANDWIDTH_NARROWBAND</tt>, <tt>BANDWIDTH_SUPERWIDEBAND</tt> or
     *  <tt>BANDWIDTH_WIDEBAND</tt>.
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_bandwidth(long encoder, int bandwidth);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder bitrate
     *
     * @param encoder The encoder to use
     * @param bitrate The bitrate to set
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_bitrate(long encoder, int bitrate);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder complexity setting.
     *
     * @param encoder The encoder to use
     * @param complexity The complexity level, from 1 to 10
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_complexity(long encoder,
                                                    int complexity);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * DTX setting of the encoder.
     *
     * @param encoder The encoder to use
     * @param dtx 0 to turn DTX off, non-zero to turn it on
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_dtx(long encoder, int dtx);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * force channels setting of the encoder.
     *
     * @param encoder The encoder to use
     * @param forcechannels Number of channels
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_force_channels(long encoder,
                                                        int forcechannels);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder FEC setting.
     * @param encoder The encoder to use
     * @param inbandFEC 0 to turn FEC off, non-zero to turn it on.
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_inband_fec(long encoder,
                                                    int inbandFEC);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * maximum audio bandwidth to be used by the encoder.
     *
     * @param encoder The encoder to use
     * @param maxBandwidth The maximum bandwidth to use, should be one of
     *  <tt>BANDWIDTH_FULLBAND</tt>, <tt>BANDWIDTH_MEDIUMBAND</tt>,
     *  <tt>BANDWIDTH_NARROWBAND</tt>, <tt>BANDWIDTH_SUPERWIDEBAND</tt> or
     *  <tt>BANDWIDTH_WIDEBAND</tt>
     *
     * @return <tt>OPUS_OK</tt> on success.
     */
    public static native int encoder_set_max_bandwidth(long encoder,
                                                       int maxBandwidth);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder's expected packet loss percentage.
     *
     * @param encoder The encoder to use
     * @param packetLossPerc
     *
     * @return OPUS_OK on success.
     */
    public static native int encoder_set_packet_loss_perc(long encoder,
                                                          int packetLossPerc);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder VBR setting
     *
     * @param encoder The encoder to use
     * @param vbr 0 to turn VBR off, non-zero to turn it on.
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_vbr(long encoder, int vbr);

    /**
     * Wrapper around the native <tt>opus_encoder_ctl</tt> function. Sets the
     * encoder VBR constraint setting
     *
     * @param encoder The encoder to use
     * @param use_cvbr 0 to turn VBR constraint off, non-zero to turn it on.
     *
     * @return OPUS_OK on success
     */
    public static native int encoder_set_vbr_constraint(long encoder,
                                                        int use_cvbr);

    /**
     * Returns the audio bandwidth of an Opus packet, one of
     * <tt>BANDWIDTH_FULLBAND</tt>, <tt>BANDWIDTH_MEDIUMBAND</tt>,
     * <tt>BANDWIDTH_NARROWBAND</tt>, <tt>BANDWIDTH_SUPERWIDEBAND</tt> or
     * <tt>BANDWIDTH_WIDEBAND</tt>, or <tt>INVALID_PACKET</tt> on error.
     *
     * @param data Array holding the packet.
     * @param offset Offset into packet where the actual packet begins.
     *
     * @return one of  <tt>BANDWIDTH_FULLBAND</tt>,
     * <tt>BANDWIDTH_MEDIUMBAND</tt>, <tt>BANDWIDTH_NARROWBAND</tt>,
     * <tt>BANDWIDTH_SUPERWIDEBAND</tt>, <tt>BANDWIDTH_WIDEBAND</tt>,
     * or <tt>INVALID_PACKET</tt> on error.
     */
    public static native int packet_get_bandwidth(byte[] data, int offset);

    /**
     * Returns the number of channels encoded in an Opus packet.
     *
     * @param data Array holding the packet.
     * @param offset Offset into packet where the actual packet begins.
     *
     * @return the number of channels encoded in <tt>data</tt>.
     */
    public static native int packet_get_nb_channels(byte[] data, int offset);

    /**
     * Returns the number of frames in an Opus packet.
     *
     * @param packet Array holding the packet.
     * @param offset Offset into packet where the actual packet begins.
     * @param length Length of the packet.
     *
     * @return the number of frames in <tt>packet</tt>.
     */
    public static native int packet_get_nb_frames(byte[] packet, int offset,
                                                  int length);
}
