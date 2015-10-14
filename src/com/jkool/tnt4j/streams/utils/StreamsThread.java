/**
 *
 */

package com.jkool.tnt4j.streams.utils;

import org.apache.log4j.Logger;

/**
 * Base class for Streams threads.
 *
 * @version $Revision: 12 $
 * @see java.lang.Thread
 */
public class StreamsThread extends Thread
{
  private static final Logger prvLogger = Logger.getLogger (StreamsThread.class);

  protected Logger logger = null;
  protected boolean stopRunning = false;

  /**
   * Creates new Streams thread.
   *
   * @see java.lang.Thread()
   */
  public StreamsThread ()
  {
    super ();
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param target object whose <code>run</code> method is called
   *
   * @see java.lang.Thread(java.lang.Runnable)
   */
  public StreamsThread (Runnable target)
  {
    super (target);
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param target object whose <code>run</code> method is called
   *
   * @see java.lang.Thread(java.lang.Runnable, java.lang.String)
   */
  public StreamsThread (Runnable target, String name)
  {
    super (target, name);
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param name name of thread
   *
   * @see java.lang.Thread(java.lang.String)
   */
  public StreamsThread (String name)
  {
    super (name);
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param threadGrp thread group to add new thread to
   * @param target    object whose <code>run</code> method is called
   *
   * @see java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable)
   */
  public StreamsThread (ThreadGroup threadGrp, Runnable target)
  {
    super (threadGrp, target);
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param threadGrp thread group to add new thread to
   * @param target    object whose <code>run</code> method is called
   * @param name      name of thread
   *
   * @see java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String)
   */
  public StreamsThread (ThreadGroup threadGrp, Runnable target, String name)
  {
    super (threadGrp, target, name);
    setDefaultName ();
  }

  /**
   * Creates new Streams thread.
   *
   * @param threadGrp thread group to add new thread to
   * @param name      name of thread
   *
   * @see java.lang.Thread(java.lang.ThreadGroup, java.lang.String)
   */
  public StreamsThread (ThreadGroup threadGrp, String name)
  {
    super (threadGrp, name);
    setDefaultName ();
  }

  /**
   * <p>Sets default name for a Streams thread.</p>
   * <p>Prefixes current (default) thread name with thread's ID and strips off leading
   * "com.jkool.tnt4j.streams." from thread name. </p>
   */
  protected void setDefaultName ()
  {
    setName (getId () + ":" + getName ().replaceFirst ("com.jkool.tnt4j.streams.", ""));
  }

  /**
   * Indicates whether the thread was signaled to stop running.
   *
   * @return <code>true</code> if thread signaled to stop, <code>false</code> if not
   */
  public boolean isStopRunning ()
  {
    return stopRunning;
  }

  /**
   * Stops this thread.
   */
  public void halt ()
  {
    if (logger != null && logger.isDebugEnabled ())
    {
      logger.debug ("Signaled to terminate");
    }
    stopRunning = true;
    interrupt ();
  }

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution)
   * for the specified number of milliseconds.  This method differs from
   * {@link java.lang.Thread#sleep(long)} (which it uses) in that it does not
   * throw any exceptions.  If the sleep is interrupted, then this method will
   * just return.
   *
   * @param millis the length of time to sleep in milliseconds
   *
   * @see java.lang.Thread#sleep(long)
   */
  public static void sleep (long millis)
  {
    long startTime = System.currentTimeMillis ();
    try
    {
      Thread.sleep (millis);
    }
    catch (InterruptedException e)
    {
      if (prvLogger.isDebugEnabled ())
      {
        prvLogger.debug ("Sleep interrupted after " + (System.currentTimeMillis () - startTime) + " msec (initial=" + millis + ")");
      }
    }
  }

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution)
   * for the specified number of milliseconds.  This method differs from
   * {@link java.lang.Thread#sleep(long)} (which it uses) in that it does not
   * throw any exceptions, and if the sleep is interrupted other than to signal
   * thread to terminate, this method will cause current thread to go "back to sleep"
   * for the remainder of the time.
   *
   * @param millis the length of time to sleep in milliseconds
   *
   * @see java.lang.Thread#sleep(long)
   */
  public void sleepFully (long millis)
  {
    long startTime = 0;
    long remainMillis = millis;
    int interruptCount = 0;
    while (remainMillis > 0)
    {
      try
      {
        startTime = System.currentTimeMillis ();
        Thread.sleep (remainMillis);
        remainMillis = 0;    // not interrupted, stop sleeping
      }
      catch (InterruptedException e)
      {
        // if sleep interrupted because thread was signaled to terminate, then return
        if (stopRunning)
        {
          return;
        }
        // subtract from sleep time the amount of time we were actually asleep
        long sleepMillis = System.currentTimeMillis () - startTime;
        remainMillis -= sleepMillis;
        interruptCount++;
        if (logger.isDebugEnabled ())
        {
          logger.debug ("Sleep interrupted (count=" + interruptCount + "), after " + sleepMillis + " msec (initial=" + millis + ")");
          if (remainMillis > 0)
          {
            logger.debug ("   Going back to sleep for " + remainMillis + " msec");
          }
        }
      }
    }
  }

  /**
   * Waits at most millis milliseconds for this thread to die. A timeout of 0 means to wait forever.
   * Differs from {@code java.lang.Thread#join()}, which it wraps, in that it does not throw an
   * exception when interrupted.
   *
   * @param millis time to wait in milliseconds
   *
   * @see java.lang.Thread#join()
   */
  public void waitFor (long millis)
  {
    long startTime = System.currentTimeMillis ();
    try
    {
      join (millis);
    }
    catch (InterruptedException e) {}
    if (prvLogger.isDebugEnabled ())
    {
      prvLogger.debug ("Completed waiting for thread to die in " + (System.currentTimeMillis () - startTime) + " msec");
    }
  }

  /**
   * Gets debug logger for thread.
   *
   * @return debug logger
   */
  public Logger getDbgLogger ()
  {
    return logger;
  }

  /**
   * Sets debug logger for thread.
   * param logger debug logger
   */
  public void setDbgLogger (Logger logger)
  {
    this.logger = logger;
  }
}
