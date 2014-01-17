/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "org_jitsi_impl_neomedia_codec_audio_speex_Speex.h"

#include <speex/speex.h>
#include <speex/speex_resampler.h>
#include <stdint.h>
#include <stdlib.h>

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1destroy
    (JNIEnv *env, jclass clazz, jlong bits)
{
    SpeexBits *bitsPtr = (SpeexBits *) (intptr_t) bits;

    speex_bits_destroy(bitsPtr);
    free(bitsPtr);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1init
    (JNIEnv *env, jclass clazz)
{
    SpeexBits *bits = malloc(sizeof(SpeexBits));

    if (bits)
        speex_bits_init(bits);
    return (jlong) (intptr_t) bits;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1nbytes
    (JNIEnv *env, jclass clazz, jlong bits)
{
    return speex_bits_nbytes((SpeexBits *) (intptr_t) bits);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1read_1from
    (JNIEnv *env, jclass clazz,
        jlong bits, jbyteArray bytes, jint bytesOffset, jint len)
{
    jbyte *bytesPtr = (*env)->GetPrimitiveArrayCritical(env, bytes, NULL);

    if (bytesPtr)
    {
        speex_bits_read_from(
            (SpeexBits *) (intptr_t) bits,
            (char *) (bytesPtr + bytesOffset),
            len);
        (*env)->ReleasePrimitiveArrayCritical(env, bytes, bytesPtr, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1remaining
    (JNIEnv *env, jclass clazz, jlong bits)
{
    return speex_bits_remaining((SpeexBits *) (intptr_t) bits);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1reset
    (JNIEnv *env, jclass clazz, jlong bits)
{
    speex_bits_reset((SpeexBits *) (intptr_t) bits);
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_bits_1write
    (JNIEnv *env, jclass clazz,
        jlong bits, jbyteArray bytes, jint bytesOffset, jint max_len)
{
    jbyte *bytes_ = (*env)->GetPrimitiveArrayCritical(env, bytes, NULL);
    jint ret;

    if (bytes_)
    {
        ret
            = speex_bits_write(
                (SpeexBits *) (intptr_t) bits,
                (char *) (bytes_ + bytesOffset),
                max_len);
        (*env)->ReleasePrimitiveArrayCritical(env, bytes, bytes_, 0);
    }
    else
        ret = 0;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_decode_1int
    (JNIEnv *env, jclass clazz,
        jlong state, jlong bits, jshortArray out, jint outOffset)
{
    jshort *out_ = (*env)->GetPrimitiveArrayCritical(env, out, NULL);
    jint ret;

    if (out_)
    {
        ret
            = speex_decode_int(
                (void *) (intptr_t) state,
                (SpeexBits *) (intptr_t) bits,
                (spx_int16_t *) (out_ + outOffset));
        (*env)->ReleasePrimitiveArrayCritical(env, out, out_, 0);
    }
    else
        ret = -2;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_decoder_1ctl__JI
    (JNIEnv *env, jclass clazz, jlong state, jint request)
{
    int ret;
    int value = 0;

    ret = speex_decoder_ctl((void *) (intptr_t) state, request, &value);
    if (ret == 0)
        ret = value;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_decoder_1ctl__JII
    (JNIEnv *env, jclass clazz, jlong state, jint request, jint value)
{
    return speex_decoder_ctl((void *) (intptr_t) state, request, &value);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_decoder_1destroy
    (JNIEnv *env, jclass clazz, jlong state)
{
    speex_decoder_destroy((void *) (intptr_t) state);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_decoder_1init
    (JNIEnv *env, jclass clazz, jlong mode)
{
    return (jlong) (intptr_t) speex_decoder_init((SpeexMode *) (intptr_t) mode);
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_encode_1int
    (JNIEnv *env, jclass clazz,
        jlong state, jshortArray in, jint inOffset, jlong bits)
{
    jshort *in_ = (*env)->GetPrimitiveArrayCritical(env, in, NULL);
    jint ret;

    if (in_)
    {
        ret
            = speex_encode_int(
                (void *) (intptr_t) state,
                (spx_int16_t *) (in_ + inOffset),
                (SpeexBits *) (intptr_t) bits);
        (*env)->ReleasePrimitiveArrayCritical(env, in, in_, JNI_ABORT);
    }
    else
        ret = 0;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_encoder_1ctl__JI
    (JNIEnv *env, jclass clazz, jlong state, jint request)
{
    int ret;
    int value = 0;

    ret = speex_encoder_ctl((void *) (intptr_t) state, request, &value);
    if (ret == 0)
        ret = value;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_encoder_1ctl__JII
    (JNIEnv *env, jclass clazz, jlong state, jint request, jint value)
{
    return speex_encoder_ctl((void *) (intptr_t) state, request, &value);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_encoder_1destroy
    (JNIEnv *env, jclass clazz, jlong state)
{
    speex_encoder_destroy((void *) (intptr_t) state);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_encoder_1init
    (JNIEnv *env, jclass clazz, jlong mode)
{
    return (jlong) (intptr_t) speex_encoder_init((SpeexMode *) (intptr_t) mode);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_lib_1get_1mode
    (JNIEnv *env, jclass clazz, jint mode)
{
    return (jlong) (intptr_t) speex_lib_get_mode(mode);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_resampler_1destroy
    (JNIEnv *env, jclass clazz, jlong state)
{
    speex_resampler_destroy((SpeexResamplerState *) (intptr_t) state);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_resampler_1init
    (JNIEnv *env, jclass clazz,
    jint nb_channels, jint in_rate, jint out_rate, jint quality, jlong err)
{
    return
        (jlong)
        (intptr_t)
            speex_resampler_init(
                nb_channels,
                in_rate, out_rate,
                quality,
                (int *) (intptr_t) err);
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_resampler_1process_1interleaved_1int
    (JNIEnv *env, jclass clazz,
        jlong state,
        jobject in, jint inOffsetInBytes, jint inSampleCount,
        jobject out, jint outOffsetInBytes, jint outSampleCount)
{
    jbyte *in_ = (*env)->GetPrimitiveArrayCritical(env, in, NULL);
    jint ret;

    if (in_)
    {
        jbyte *out_ = (*env)->GetPrimitiveArrayCritical(env, out, NULL);

        if (out_)
        {
            spx_uint32_t in_len = inSampleCount;
            spx_uint32_t out_len = outSampleCount;

            ret
                = speex_resampler_process_interleaved_int(
                    (SpeexResamplerState *) (intptr_t) state,
                    (spx_int16_t *) (in_ + inOffsetInBytes),
                    &in_len,
                    (spx_int16_t *) (out_ + outOffsetInBytes),
                    &out_len);
            (*env)->ReleasePrimitiveArrayCritical(env, out, out_, 0);

            /*
             * speex_resampler_process_interleaved_int is supposed to return the
             * number of samples which have been written but it doesn't seem to
             * do it and instead returns zero.
             */
            ret = out_len;
        }
        else
            ret = 0;
        (*env)->ReleasePrimitiveArrayCritical(env, in, in_, JNI_ABORT);
    }
    else
        ret = 0;
    return ret;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_speex_Speex_resampler_1set_1rate
    (JNIEnv *env, jclass clazz, jlong state, jint in_rate, jint out_rate)
{
    return
        speex_resampler_set_rate(
            (SpeexResamplerState *) (intptr_t) state,
            in_rate, out_rate);
}
