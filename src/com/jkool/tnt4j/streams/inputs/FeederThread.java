/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.inputs;

import com.jkool.tnt4j.streams.utils.StreamsThread;

/**
 * Base class for threads running an ActivityFeeder.
 *
 * @version $Revision: 3 $
 * @see ActivityFeeder
 */
public class FeederThread extends StreamsThread
{

  /**
   * ActivityFeeder being executed by this thread.
   */
  protected ActivityFeeder target;

  /**
   * Creates thread to run specified ActivityFeeder.
   *
   * @param target the ActivityFeeder to run
   *
   * @see java.lang.Thread#Thread(Runnable)
   */
  public FeederThread (ActivityFeeder target)
  {
    super (target);
    this.target = target;
    target.setOwnerThread (this);
    setDbgLogger (target.getDbgLogger ());
  }

  /**
   * Creates thread to run specified ActivityFeeder.
   *
   * @param target the ActivityFeeder to run
   * @param name   the name for thread
   *
   * @see java.lang.Thread#Thread(Runnable, String)
   */
  public FeederThread (ActivityFeeder target, String name)
  {
    super (target, name);
    this.target = target;
    target.setOwnerThread (this);
    setDbgLogger (target.getDbgLogger ());
  }

  /**
   * Creates thread to run specified ActivityFeeder.
   *
   * @param group  the thread group new thread is to belong to
   * @param target the ActivityFeeder to run
   * @param name   the name for thread
   *
   * @see java.lang.Thread#Thread(ThreadGroup, Runnable, String)
   */
  public FeederThread (ThreadGroup group, ActivityFeeder target, String name)
  {
    super (group, target, name);
    this.target = target;
    target.setOwnerThread (this);
    setDbgLogger (target.getDbgLogger ());
  }

  /**
   * Creates thread to run specified ActivityFeeder.
   *
   * @param group  the thread group new thread is to belong to
   * @param target the ActivityFeeder to run
   *
   * @see java.lang.Thread#Thread(ThreadGroup, Runnable)
   */
  public FeederThread (ThreadGroup group, ActivityFeeder target)
  {
    super (group, target);
    this.target = target;
    target.setOwnerThread (this);
    setDbgLogger (target.getDbgLogger ());
  }

  /**
   * Gets the ActivityFeeder being run by this thread.
   *
   * @return ActivityFeeder being run
   */
  public ActivityFeeder getTarget ()
  {
    return target;
  }
}
