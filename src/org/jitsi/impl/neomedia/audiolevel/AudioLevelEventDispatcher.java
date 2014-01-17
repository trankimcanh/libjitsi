/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.audiolevel;

import javax.media.*;

import org.jitsi.service.neomedia.event.*;

/**
 * The class implements an audio level measurement thread. The thread will
 * measure new data every time it is added through the <tt>addData()</tt> method
 * and would then deliver it to a registered listener if any. (No measurement
 * would be performed until we have a <tt>levelListener</tt>). We use a
 * separate thread so that we could compute and deliver audio levels in a way
 * that won't delay the media processing thread.
 * <p>
 * Note that, for performance reasons this class is not 100% thread safe and you
 * should not modify add or remove audio listeners in this dispatcher in the
 * notification thread (i.e. in the thread where you were notified of an audio
 * level change).
 *
 * @author Damian Minkov
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class AudioLevelEventDispatcher
{
    /**
     * The interval of time in milliseconds which the <tt>Thread</tt> of
     * <tt>AudioLevelEventDispatcher</tt> is to idly wait for data to be
     * provided for audio level calculation before it exits.
     */
    private static final long IDLE_TIMEOUT = 30 * 1000;

    /**
     * The audio samples provided to this instance as a <tt>byte</tt> array to
     * measure the audio levels of.
     */
    private byte[] bytes;

    /**
     * The length of the audio samples in {@link #bytes}.
     */
    private int bytesLength = 0;

    /**
     * The <tt>AudioLevelMap</tt> in which the audio calculations run by this
     * <tt>AudioLevelEventDispatcher</tt> are to be cached in addition to
     * dispatching them to {@link #listener}.
     */
    private AudioLevelMap cache = null;

    /**
     * This is the last level we fired.
     */
    private int lastLevel = 0;

    /**
     * The listener which is interested in audio level changes.
     */
    private SimpleAudioLevelListener listener;

    /**
     * The audio samples provided to this instance as a <tt>short</tt> array to
     * measure the audio levels of.
     */
    private short[] shorts;

    /**
     * The length of the audio samples in {@link #bytes}.
     */
    private int shortsLength = 0;

    /**
     * The SSRC of the stream we are measuring that we should use as a key for
     * entries of the levelMap level cache.
     */
    private long ssrc = -1;

    /**
     * The <tt>Thread</tt> which runs the actual audio level calculations and
     * dispatches to {@link #listener}.
     */
    private Thread thread;

    /**
     * The name of the <tt>Thread</tt> which is to run the actual audio level
     * calculations and to dispatch to {@link #listener}.
     */
    private final String threadName;

    /**
     * Initializes a new <tt>AudioLevelEventDispatcher</tt> instance which is to
     * use a specific name for its <tt>Thread</tt> which is to run the actual
     * audio level calculations and to dispatch to its
     * <tt>SimpleAudioLevelListener</tt>
     *
     * @param threadName
     */
    public AudioLevelEventDispatcher(String threadName)
    {
        this.threadName = threadName;
    }

    /**
     * Adds data to be processed.
     *
     * @param buffer the data that we'd like to queue for processing.
     */
    public synchronized void addData(Buffer buffer)
    {
        /*
         * If no one is interested in the audio level, do not even add the
         * Buffer data.
         */
        if ((listener == null) && ((cache == null) || (ssrc == -1)))
            return;

        int length = buffer.getLength();

        if (length > 0)
        {
            Object src = buffer.getData();

            if (src != null)
            {
                Object dst;

                if (src instanceof short[])
                {
                    if ((shorts == null) || (shorts.length < length))
                        shorts = new short[length];
                    dst = shorts;
                    bytesLength = 0;
                    shortsLength = length;
                }
                else if (src instanceof byte[])
                {
                    if ((bytes == null) || (bytes.length < length))
                        bytes = new byte[length];
                    dst = bytes;
                    bytesLength = length;
                    shortsLength = 0;
                }
                else
                {
                    bytesLength = 0;
                    shortsLength = 0;
                    return;
                }
                System.arraycopy(src, buffer.getOffset(), dst, 0, length);

                if (thread == null)
                    startThread();
                else
                    notify();
            }
        }
    }

    /**
     * Runs the actual audio level calculations and dispatches to the
     * {@link #listener}.
     */
    private void run()
    {
        long idleTimeoutStart = -1;

        do
        {
            SimpleAudioLevelListener listener;
            AudioLevelMap cache;
            long ssrc;

            byte[] bytes;
            short[] shorts;
            int length;

            synchronized(this)
            {
                if (!Thread.currentThread().equals(thread))
                    break;

                listener = this.listener;
                cache = this.cache;
                ssrc = this.ssrc;
                /*
                 * If no one is interested in the audio level, do not even keep
                 * the Thread waiting.
                 */
                if ((listener == null) && ((cache == null) || (ssrc == -1)))
                    break;

                if (shortsLength > 0)
                {
                    bytes = null;
                    shorts = this.shorts;
                    length = shortsLength;
                }
                else if (bytesLength > 0)
                {
                    bytes = this.bytes;
                    shorts = null;
                    length = bytesLength;
                }
                else
                {
                    bytes = null;
                    shorts = null;
                    length = 0;
                }
                /*
                 * If there is no data to calculate the audio level of, wait for
                 * such data to be provided.
                 */
                if (((bytes == null) && (shorts == null)) || (length < 1))
                {
                    // The current thread is idle.
                    long now = System.currentTimeMillis();

                    if (idleTimeoutStart == -1)
                        idleTimeoutStart = now;
                    else if ((now - idleTimeoutStart) >= IDLE_TIMEOUT)
                        break;

                    boolean interrupted = false;

                    try
                    {
                        wait(IDLE_TIMEOUT);
                    }
                    catch (InterruptedException ie)
                    {
                        interrupted = true;
                    }
                    if (interrupted)
                        Thread.currentThread().interrupt();

                    continue;
                }
                // The values of data and dataLength seem valid so consume them.
                if (this.shorts == shorts)
                    this.shorts = null;
                else if (this.bytes == bytes)
                    this.bytes = null;
                bytesLength = 0;
                shortsLength = 0;
                // The current thread is no longer idle.
                idleTimeoutStart = -1;
            }

            int newLevel;

            if (shorts != null)
            {
                newLevel
                    = AudioLevelCalculator.calculateSoundPressureLevel(
                            shorts, 0, length,
                            SimpleAudioLevelListener.MIN_LEVEL,
                            SimpleAudioLevelListener.MAX_LEVEL,
                            lastLevel);
            }
            else if (bytes != null)
            {
                newLevel
                    = AudioLevelCalculator.calculateSoundPressureLevel(
                            bytes, 0, length,
                            SimpleAudioLevelListener.MIN_LEVEL,
                            SimpleAudioLevelListener.MAX_LEVEL,
                            lastLevel);
            }
            else
            {
                newLevel = 0;
            }

            /*
             * In order to try to mitigate the issue with allocating data, try
             * to return the one which we have just calculated the audio level
             * of.
             */
            synchronized (this)
            {
                if (shorts != null)
                {
                    if (this.shorts == null)
                        this.shorts = shorts;
                }
                else if (bytes != null)
                {
                    if (this.bytes == null)
                        this.bytes = bytes;
                }
            }

            try
            {
                // Cache the newLevel if requested.
                if ((cache != null) && (ssrc != -1))
                    cache.putLevel(ssrc, newLevel);
                // Notify the listener about the newLevel if requested.
                if (listener != null)
                    listener.audioLevelChanged(newLevel);
            }
            finally
            {
                lastLevel = newLevel;
            }
        }
        while (true);
    }

    /**
     * Sets an <tt>AudioLevelMap</tt> that this dispatcher could use to cache
     * levels it's measuring in addition to simply delivering them to a
     * listener.
     *
     * @param cache the <tt>AudioLevelMap</tt> where this dispatcher should
     * cache measured results.
     * @param ssrc the SSRC key where entries should be logged
     */
    public synchronized void setAudioLevelCache(AudioLevelMap cache, long ssrc)
    {
        if ((this.cache != cache) || (this.ssrc != ssrc))
        {
            this.cache = cache;
            this.ssrc = ssrc;
            startOrNotifyThread();
        }
    }

    /**
     * Sets the new listener that will be gathering all events from this
     * dispatcher.
     *
     * @param listener the listener that we will be notifying or <tt>null</tt>
     * if we are to remove it.
     */
    public synchronized void setAudioLevelListener(
            SimpleAudioLevelListener listener)
    {
        if (this.listener != listener)
        {
            this.listener = listener;
            startOrNotifyThread();
        }
    }

    /**
     * Starts the <tt>Thread</tt> which is to run the audio level calculations
     * and to dispatch to {@link #listener} if necessary or notifies it about a
     * change it the state on which it depends.
     */
    private synchronized void startOrNotifyThread()
    {
        if ((this.listener == null) && ((cache == null) || (ssrc == -1)))
        {
            thread = null;
            notify();
        }
        else if (((shorts != null) && (shortsLength > 0))
                || ((bytes != null) && (bytesLength > 0)))
        {
            if (thread == null)
                startThread();
            else
                notify();
        }
    }

    /**
     * Starts the <tt>Thread</tt> which is to run the audio level calculations
     * and to dispatch to {@link #listener}.
     */
    private synchronized void startThread()
    {
        thread
            = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        AudioLevelEventDispatcher.this.run();
                    }
                    finally
                    {
                        synchronized (AudioLevelEventDispatcher.this)
                        {
                            if (Thread.currentThread().equals(thread))
                                thread = null;
                            /*
                             * If the thread of this AudioLevelEventDispatcher
                             * is dying yet the state suggests that it should be
                             * running, restart it.
                             */
                            if ((thread == null)
                                    && ((listener != null)
                                            || ((cache != null)
                                                    && (ssrc != -1)))
                                    && (((bytes != null) && (bytesLength > 0))
                                            || ((shorts != null)
                                                    && (shortsLength > 0))))
                                startThread();
                        }
                    }
                }
            };
        thread.setDaemon(true);
        if (threadName != null)
            thread.setName(threadName);

        thread.start();
    }
}
