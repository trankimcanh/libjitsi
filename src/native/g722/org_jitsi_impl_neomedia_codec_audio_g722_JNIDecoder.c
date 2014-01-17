/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "org_jitsi_impl_neomedia_codec_audio_g722_JNIDecoder.h"

#include <inttypes.h>
#include <stdint.h>

#include "telephony.h"
#include "g722.h"

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1close
    (JNIEnv *env, jclass clazz, jlong decoder)
{
    g722_decode_state_t *d = (g722_decode_state_t *) (intptr_t) decoder;

    g722_decode_release(d);
    g722_decode_free(d);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1open
    (JNIEnv *env, jclass clazz)
{
    return (jlong) (intptr_t) g722_decode_init(NULL, 64000, 0);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1process
    (JNIEnv *env, jclass clazz,
        jlong decoder,
        jbyteArray in, jint inOffset,
        jshortArray out, jint outOffset, jint outLength)
{
    jshort *out_ = (*env)->GetPrimitiveArrayCritical(env, out, NULL);

    if (out_)
    {
        jbyte *in_ = (*env)->GetPrimitiveArrayCritical(env, in, NULL);

        if (in_)
        {
            g722_decode(
                    (g722_decode_state_t *) (intptr_t) decoder,
                    (int16_t *) (out_ + outOffset),
                    (const uint8_t *) (in_ + inOffset),
                    outLength / 2);
            (*env)->ReleasePrimitiveArrayCritical(env, in, in_, JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, out, out_, 0);
    }
}
