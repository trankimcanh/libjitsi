/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "org_jitsi_impl_neomedia_codec_audio_g722_JNIEncoder.h"

#include <inttypes.h>
#include "telephony.h"
#include "g722.h"

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1close
    (JNIEnv *env, jclass clazz, jlong encoder)
{
    g722_encode_state_t *e = (g722_encode_state_t *) (intptr_t) encoder;

    g722_encode_release(e);
    g722_encode_free(e);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1open
    (JNIEnv *env, jclass clazz)
{
    return (jlong) (intptr_t) g722_encode_init(NULL, 64000, 0);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1process
    (JNIEnv *env, jclass clazz,
        jlong encoder,
        jshortArray in, jint inOffset,
        jbyteArray out, jint outOffset, jint outLength)
{
    jbyte *out_ = (*env)->GetPrimitiveArrayCritical(env, out, NULL);

    if (out_)
    {
        jshort *in_ = (*env)->GetPrimitiveArrayCritical(env, in, NULL);

        if (in_)
        {
            g722_encode(
                    (g722_encode_state_t *) (intptr_t) encoder,
                    (uint8_t *) (out_ + outOffset),
                    (const int16_t *) (in_ + inOffset),
                    2 * outLength);
            (*env)->ReleasePrimitiveArrayCritical(env, in, in_, JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, out, out_, 0);
    }
}
