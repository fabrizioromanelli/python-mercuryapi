/*
 * Copyright (c) 2008 ThingMagic, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.thingmagic;

import java.util.Vector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;

/**
 * The Reader class encapsulates a connection to a ThingMagic RFID
 * reader device and provides an interface to perform RFID operations
 * such as reading tags and writing tag IDs. Reads can be done on
 * demand, with the {@link #read} method, or continuously in the
 * background with the {@link #startReading} method. Background reads
 * notify a listener objects of tags that are read.
 *
 * Methods which communicate with the reader can throw ReaderException
 * if the communication breaks down. Other reasons for throwing
 * ReaderException are documented in the individual methods.
 *
 * Operations which take an argument for a tag to operate on may
 * optionally be passed a null argument. This lets the reader choose
 * what tag to use, but may not work if multiple tags are
 * present. This use is recommended only when exactly one tag is known
 * to be in range.
 */
abstract public class Reader
{

  protected List<ReadListener> readListeners;
  protected List<ReadExceptionListener> readExceptionListeners;
  boolean connected;
  Thread readerThread, notifierThread, exceptionNotifierThread;
  BackgroundReader backgroundReader;
  BackgroundNotifier backgroundNotifier;
  ExceptionNotifier exceptionNotifier;
  final BlockingQueue<TagReadData> tagReadQueue;
  final BlockingQueue<ReaderException> exceptionQueue;
  Map<String,Setting> params;

  /**
   * RFID regulatory regions
   */
  public enum Region
  {
      /** European Union */ EU, 
      /** India */ IN,
      /** Japan */ JP, 
      /** Korea */ KR, 
      /** Korea (revised) */ KR2, 
      /** North America */ NA, 
      /** China */ PRC,
      /** European Union (revised) */ EU2, 
      /** European Union (revised) */ EU3, 
      /** No-limit region */ OPEN,
      /** No region */ NONE;
  }

  protected void checkConnection()
    throws ReaderException
  {
    if (connected == false)
    {
      throw new ReaderException("No reader connected to reader object");
    }
  }

  // Level 1 API
  // note no public constructor
  Reader()
  {
    readListeners = new Vector<ReadListener>();
    readExceptionListeners = new Vector<ReadExceptionListener>();
    connected = false;
    
    tagReadQueue = new LinkedBlockingQueue<TagReadData>();
    exceptionQueue = new LinkedBlockingQueue<ReaderException>();

    initparams();
  }

  /**
   * Return an instance of a Reader class that is associated with a
   * RFID reader on a particular communication channel. The
   * communication channel is not established until the connect()
   * method is called. Note that some readers may need parameters
   * (such as the regulatory region) set before the connect will
   * succeed.
   *
   * @param uriString an identifier for the reader to connect to, with
   * a URI syntax. The scheme can be <tt>eapi</tt> for the embedded
   * module protocol, <tt>rql</tt> for the request query language, or
   * <tt>tmr</tt> to guess. The remainder of the URI identifies the
   * stream that the protocol will be spoken over, either a local host
   * serial port device or a TCP network port.
   *
   *  Examples include:<tt>
   *  <ul>
   *  <li>"eapi:///dev/ttyUSB0"
   *  <li>"eapi:///com1"
   *  <li>"rql://reader.example.com/"
   *  <li>"rql://proxy.example.com:2500/"
   *  <li>"tmr:///dev/ttyS0"
   *  <li>"tmr://192.168.1.101/"
   *  </ul></tt>
   *
   * @return the {@link Reader} object connected to the specified device
   * @throws ReaderException if reader initialization failed
   */
  public static Reader create(String uriString)
    throws ReaderException
  {
    URI uri;
    Reader reader;

    try
    {
      uri = new URI(uriString);
    } 
    catch (URISyntaxException e)
    {
      throw new ReaderException(e.getMessage());
    }

    String scheme = uri.getScheme();
    String authority = uri.getAuthority();
    String path = uri.getPath();
    String host = uri.getHost();
    int port = uri.getPort();

    if (scheme == null)
    {
      throw new ReaderException("Blank URI scheme");
    }

    if (scheme.equals("tmr"))
    {
      if (host != null)
      {
        scheme = "rql";
      }
      else
      {
        scheme = "eapi";
      }
    }

    if (scheme.equals("eapi"))
    {
      if (authority != null && !authority.equals(""))
      {
        throw new ReaderException("Remote hosts not supported for " + scheme);
      }

      reader = new SerialReader(path);
    }
    else if (scheme.equals("llrp+eapi"))
    {
      if (!(path.equals("") || path.equals("/")))
      {
        throw new ReaderException("Path not supported for " + scheme);
      }

      SerialTransport st;
      if (port == -1)
      {
        st = new LlrpTransport(host, 5084);
      }
      else
      {
        st = new LlrpTransport(host, port);
      }

      reader = new SerialReader(st);
    }
    else if (scheme.equals("rql"))
    {
      if (!(path.equals("") || path.equals("/")))
      {
        throw new ReaderException("Path not supported for " + scheme);
      }

      if (port == -1)
      {
        reader = new RqlReader(host);
      }
      else
      {
        reader = new RqlReader(host, port);
      }
    }
    else
    {
      throw new ReaderException("Unknown URI scheme");
    }

    return reader;
  }

  /**
   * Open the communication channel and initialize the session with
   * the reader.
   */
  public abstract void connect()
    throws ReaderException;

  /**
   * Shuts down the connection with the reader device.
   */
  public abstract void destroy();

  /**
   * Read RFID tags for a fixed duration.
   *
   * @param duration the time to spend reading tags, in milliseconds
   * @return the tags read
   * @see TagReadData
   */ 
  public abstract TagReadData[] read(long duration)
    throws ReaderException;

  /**
   * Read data from the memory bank of a tag. 
   *
   * @param target the tag to read from, or null
   * @param bank the tag memory bank to read from
   * @param address the byte address to start reading at
   * @param count the number of bytes to read
   * @return the bytes read
   *
   */
  public abstract byte[] readTagMemBytes(TagFilter target,
                                         int bank, int address, int count)
    throws ReaderException;


  /**
   * Read data from the memory bank of a tag. 
   *
   * @param target the tag to read from, or null
   * @param bank the tag memory bank to read from
   * @param address the word address to start reading from
   * @param count the number of words to read
   * @return the words read
   *
   */
  public abstract short[] readTagMemWords(TagFilter target,
                                          int bank, int address, int count)
    throws ReaderException;

  /**
   * Write data to the memory bank of a tag.
   *
   * @param target the tag to write to, or null
   * @param bank the tag memory bank to write to
   * @param address the byte address to start writing to
   * @param data the bytes to write
   *
   */
  public abstract void writeTagMemBytes(TagFilter target,
                                        int bank, int address, byte[] data)
    throws ReaderException;


  /**
   * Write data to the memory bank of a tag.
   *
   * @param target the tag to read from, or null
   * @param bank the tag memory bank to write to
   * @param address the word address to start writing to
   * @param data the words to write
   *
   */
  public abstract void writeTagMemWords(TagFilter target,
                                        int bank, int address, short[] data)
    throws ReaderException;

  /**
   * Write a new ID to a tag. 
   *
   * @param target the tag to write to, or null
   * @param newID the new tag ID to write
   */
  public abstract void writeTag(TagFilter target, TagData newID)
    throws ReaderException;

  /**
   * Perform a lock or unlock operation on a tag. The first tag seen
   * is operated on - the singulation parameter may be used to control
   * this. Note that a tag without an access password set may not
   * accept a lock operation or remain locked.
   *
   * @param target the tag to lock, or null
   * @param lock the locking action to take.
   */
  public abstract void lockTag(TagFilter target, TagLockAction lock)
    throws ReaderException;

  /**
   * Kill a tag. The first tag seen is killed.
   *
   * @param target the tag kill, or null
   * @param auth the authentication needeed to kill the tag
   */
  public abstract void killTag(TagFilter target, TagAuthentication auth)
    throws ReaderException;

  /** 
   * Register a listener to be notified of asynchronous RFID read events.
   *
   * @param listener the ReadListener to add
   */
  public void addReadListener(ReadListener listener)
  {
    readListeners.add(listener);
  }

  /** 
   * Remove an listener from the list of listeners notified of asynchronous
   * RFID read events.
   *
   * @param listener the ReadListener to remove
   */
  public void removeReadListener(ReadListener listener)
  {
    readListeners.remove(listener);
  }


  /** 
   * Register a listener to be notified of asynchronous RFID read exceptions.
   *
   * @param listener the ReadExceptionListener to add
   */
  public void addReadExceptionListener(ReadExceptionListener listener)
  {
    readExceptionListeners.add(listener);
  }

  /** 
   * Remove a listener from the list of listeners notified of asynchronous
   * RFID read events.
   *
   * @param r the ReadExceptionListener to remove
   */
  public void removeReadExceptionListener(ReadExceptionListener listener)
  {
    readExceptionListeners.remove(listener);
  }
  
  /**
   * Start reading RFID tags in the background. The tags found will be
   * passed to the registered read listeners, and any exceptions that
   * occur during reading will be passed to the registered exception
   * listeners. Reading will continue until stopReading() is called.
   *
   * @see #addReadListener
   * @see #addReadExceptionListener
   */
  public synchronized void startReading()
  {
    if (backgroundReader == null)
    {
      backgroundReader = new BackgroundReader();
      readerThread = new Thread(backgroundReader, "background reader");
      readerThread.setDaemon(true);
      readerThread.start();
    }
    if (backgroundNotifier == null)
    {
      backgroundNotifier = new BackgroundNotifier();
      notifierThread = new Thread(backgroundNotifier, "background notifier");
      notifierThread.setDaemon(true);
      notifierThread.start();
    }
    if (exceptionNotifier == null)
    {
      exceptionNotifier = new ExceptionNotifier();
      exceptionNotifierThread = new Thread(exceptionNotifier, "exception notifier");
      exceptionNotifierThread.setDaemon(true);
      exceptionNotifierThread.start();
    }

    backgroundReader.readOn();
  }

  /**
   * Stop reading RFID tags in the background.
   */
  public void stopReading()
    throws InterruptedException
  {
      if (backgroundReader != null) {
          backgroundReader.readOff();
          backgroundReader = null;
          readerThread.interrupt();
      }
      if (backgroundNotifier != null) {
          backgroundNotifier.drainQueue();
          backgroundNotifier = null;
          notifierThread.interrupt();
      }
      if (exceptionNotifier != null) {
          exceptionNotifier.drainQueue();
          exceptionNotifier = null;
          exceptionNotifierThread.interrupt();
      }
  }

  void notifyReadListeners(TagReadData t)
  {
    for (ReadListener rl : readListeners)
    {
      rl.tagRead(this, t);
    }
  }

  void notifyExceptionListeners(ReaderException re)
  {
    for (ReadExceptionListener rel : readExceptionListeners)
    {
      rel.tagReadException(this, re);
    }
  }

  
  public static class GpioPin
  {
    // The class is immutable, so there's potential for caching the objects,
    // as per the "flyweight" pattern, if necessary.
    final public int id;
    final public boolean high;

    public GpioPin(int id, boolean high)
    {
      this.id = id;
      this.high = high;
    }

    @Override public boolean equals(Object o)
    {
      if (!(o instanceof GpioPin))
      {
        return false;
      }
      GpioPin gp = (GpioPin)o;
      return (this.id == gp.id) && (this.high == gp.high);
    }

    @Override
    public int hashCode()
    {
      int x;
      x = id * 2;
      if (high)
      {
        x++;
      }
      return x;
    }

    @Override
    public String toString()
    {
      return String.format("%1$d%2$s", this.id, this.high?"H":"L");
    }

  }

  /**
   * Get the state of all of the reader's GPI pins.
   *
   * @return array of GpioPin objects representing the state of all input pins
   */
  public abstract GpioPin[] gpiGet()
    throws ReaderException;

  /**
   * Set the state of some GPO pins.
   *
   * @param array of GpioPin objects
   */
  public abstract void gpoSet(GpioPin[] state)
    throws ReaderException;

  /*
   * Quirk of Settings: if the type is modifiable (not primitive,
   * primitive-wrapper, or a String), then you have to be careful
   * about what is returned by paramGet, so that the caller can't
   * change the reader state by changing the returned object. As
   * there's no universal deep-copy in Java, we'll all just have to do
   * it ourselves in the get operation.
   */

  interface SettingAction
  {
    Object set(Object value)
      throws ReaderException;
    Object get(Object value)
      throws ReaderException;
  }

  abstract class ReadOnlyAction implements SettingAction
  {
    public Object set(Object value)
    {
      throw new UnsupportedOperationException();
    }
    public Object get(Object value)
      throws ReaderException
    {
      return value;
    }
  }

  class Setting
  {
    String originalName;
    Class type;
    Object value;
    SettingAction action;
    boolean writable;
    boolean confirmed;

    Setting(String name, Class t, Object def, boolean w, SettingAction act, boolean confirmed)
    {
      originalName = name;
      type = t;
      value = def;
      writable = w;
      
      action = act;
      this.confirmed = confirmed;
    }
  }


  void initparams()
  {
    Setting s;

    params = new HashMap<String,Setting>();

    addParam("/reader/read/asyncOnTime",
             Integer.class, 250, true,
             new SettingAction() {

            public Object set(Object value) throws ReaderException {
                if((Integer)value<0)
                {
                throw new IllegalArgumentException("negative value not permitted");
                }
                return value;
            }

            public Object get(Object value) throws ReaderException {
                return value;
            }
        }
             );
    addParam("/reader/read/asyncOffTime",
               Integer.class, 0, true,
               new SettingAction() {

            public Object set(Object value) throws ReaderException {
                if((Integer)value<0)
                {
                throw new IllegalArgumentException("negative value not permitted");
                }
                return value;
            }

            public Object get(Object value) throws ReaderException {
                return value;
            }
        });

  }

  void addParam(String name, Class t, Object def, boolean w, SettingAction act)
  {
    if (def != null && t.isInstance(def) == false)
      throw new IllegalArgumentException("Wrong type for parameter initial value");
    Setting s = new Setting(name, t, def, w, act, true);
    params.put(name.toLowerCase(), s);
  }

  void addUnconfirmedParam(String name, Class t, Object def, boolean w, SettingAction act)
  {
    if (def != null && t.isInstance(def) == false)
      throw new IllegalArgumentException("Wrong type for parameter initial value");
    Setting s = new Setting(name, t, def, w, act, false);
    params.put(name.toLowerCase(), s);
  }

  /**
   * Get a list of the parameters available
   *
   * <p>Supported Parameters:
   * <ul>
   * <li> /reader/antenna/checkPort
   * <li> /reader/antenna/connectedPortList
   * <li> /reader/antenna/portList
   * <li> /reader/antenna/portSwitchGpos
   * <li> /reader/antenna/settlingTimeList
   * <li> /reader/antenna/txRxMap
   * <li> /reader/baudRate
   * <li> /reader/commandTimeout
   * <li> /reader/gen2/BLF
   * <li> /reader/gen2/accessPassword
   * <li> /reader/gen2/q
   * <li> /reader/gen2/session
   * <li> /reader/gen2/tagEncoding
   * <li> /reader/gen2/target
   * <li> /reader/gen2/tari
   * <li> /reader/gen2/writeMode
   * <li> /reader/gpio/inputList
   * <li> /reader/gpio/outputList
   * <li> /reader/iso180006b/BLF
   * <li> /reader/powerMode
   * <li> /reader/radio/enablePowerSave
   * <li> /reader/radio/portReadPowerList
   * <li> /reader/radio/portWritePowerList
   * <li> /reader/radio/powerMax
   * <li> /reader/radio/powerMin
   * <li> /reader/radio/readPower
   * <li> /reader/radio/temperature
   * <li> /reader/radio/writePower
   * <li> /reader/read/asyncOffTime
   * <li> /reader/read/asyncOnTime
   * <li> /reader/read/plan
   * <li> /reader/region/hopTable
   * <li> /reader/region/hopTime
   * <li> /reader/region/id
   * <li> /reader/region/lbt/enable
   * <li> /reader/region/supportedRegions
   * <li> /reader/tagReadData/recordHighestRssi
   * <li> /reader/tagReadData/reportRssiInDbm
   * <li> /reader/tagReadData/uniqueByAntenna
   * <li> /reader/tagReadData/uniqueByData
   * <li> /reader/tagop/antenna
   * <li> /reader/tagop/protocol
   * <li> /reader/transportTimeout
   * <li> /reader/userMode
   * <li> /reader/version/hardware
   * <li> /reader/version/model
   * <li> /reader/version/serial
   * <li> /reader/version/software
   * <li> /reader/version/supportedProtocols
   * </ul>
   *
   * @return an array of the parameter names
   */
  public String[] paramList()
  {
    Vector<String> nameVec = new Vector<String>();
    int i = 0;
    // Create a new container so that removing entries isn't a problem
    for (Setting s : new ArrayList<Setting>(params.values()))
    {
      if (s.confirmed == false)
      {
        if (probeSetting(s) == false)
        {
          continue;
        }
      }
      nameVec.add(s.originalName);
    }
    return nameVec.toArray(new String[nameVec.size()]);
  }

  boolean probeSetting(Setting s)
  {
    try
    {
      s.action.get(null);
      s.confirmed = true;
    }
    catch (ReaderException e)
    {
    }

    if (s.confirmed == false)
    {
      params.remove(s.originalName);
    }
      
    return s.confirmed;
  }

  /**
   * Get the value of a Reader parameter.
   *
   * @param key the parameter name
   * @return the value of the parameter, as an Object
   * @throws IllegalArgumentException if the parameter does not exist
   */
  public Object paramGet(String key)
    throws ReaderException
  {
    Setting s;

    s = params.get(key.toLowerCase());
    if (s == null)
    {
      throw new IllegalArgumentException("No parameter named '" + key + "'.");
    }

    // Maybe mention here that the parameter doesn't work here rather than
    // that it doesn't exist?
    if (s.confirmed == false && probeSetting(s) == false)
    {
      throw new IllegalArgumentException("No parameter named '" + key + "'.");
    }

    if (s.action != null)
    {
      s.value = s.action.get(s.value);
    }
    return s.value;
  }

  /**
   * Set the value of a Reader parameter.
   *
   * @param key the parameter name
   * @param value value of the parameter, as an Object
   * @throws IllegalArgumentException if the parameter does not exist,
   * is read-only, or if the Object is the wrong type for the
   * parameter.
   */
  public void paramSet(String key, Object value)
    throws ReaderException
  {
    Setting s;

    s = params.get(key.toLowerCase());
    if (s == null)
    {
      throw new IllegalArgumentException("No parameter named '" + key + "'.");
    }
    // Maybe mention here that the parameter doesn't work here rather than
    // that it doesn't exist?
    if (s.confirmed == false && probeSetting(s) == false)
    {
      throw new IllegalArgumentException("No parameter named '" + key + "'.");
    }
    if (s.writable == false)
    {
      throw new IllegalArgumentException("Parameter '" + key + "' is read-only.");
    }
    if (value != null && !s.type.isInstance(value))
    {
      throw new IllegalArgumentException("Wrong type " + value.getClass().getName() + 
                                         " for parameter '" + key + "'.");
    }
    if (s.action != null)
    {
      value = s.action.set(value);
    }
    s.value = value;
  }

  /**
   * Load a new firmware image into the device's nonvolatile memory.
   * This installs the given image data onto the device and restarts
   * it with that image. The firmware must be of an appropriate type
   * for the device. Interrupting this operation may damage the
   * reader.
   *
   * @param firmware a data stream of the firmware contents
   */
  public abstract void firmwareLoad(InputStream firmware)
    throws ReaderException, IOException;

  class BackgroundNotifier implements Runnable
  {
    public void run()
    {
      TagReadData t;
      try
      {
        while (true) {
          synchronized (tagReadQueue)
          {
            if (tagReadQueue.isEmpty())
            {
              tagReadQueue.notifyAll();
            }
          }
          t = tagReadQueue.take();
          notifyReadListeners(t);
        }
      }
      catch(InterruptedException ex)
      {
        // stopReading uses interrupt() to kill this thread;
        // users should not see that exception
      }
    }

    void drainQueue()
      throws InterruptedException
    {
      synchronized (tagReadQueue)
      {
        while (tagReadQueue.isEmpty() == false)
        {
          tagReadQueue.wait();
        }
      }
    }
  }

  class ExceptionNotifier implements Runnable
  {
    public void run()
    {
      try
      {
        while (true) {
          synchronized (exceptionQueue)
          {
            if (exceptionQueue.isEmpty())
            {
              exceptionQueue.notifyAll();
            }
          }
          ReaderException re = exceptionQueue.take();
          notifyExceptionListeners(re);
        }
      }
      catch(InterruptedException ex)
      {
        // stopReading uses interrupt() to kill this thread;
        // users should not see that exception
      }
    }

    void drainQueue()
      throws InterruptedException
    {
      synchronized (exceptionQueue)
      {
        while (exceptionQueue.isEmpty() == false)
        {
          exceptionQueue.wait();
        }
      }
    }
  }

  class BackgroundReader implements Runnable
  {
    boolean enabled, running;
    
    public void run()
    {
      TagReadData[] tags;
      int readTime, sleepTime;

      try
      {
        while (true)
        {
          synchronized (this)
          {
            running = false;
            this.notifyAll();  // Notify change in running
            while (enabled == false)
            {
              this.wait();  // Wait for enabled to change
            }
            running = true;
            this.notifyAll();  // Notify change in running
          }
          try
          {
            readTime = (Integer)paramGet("/reader/read/asyncOnTime");
            sleepTime = (Integer)paramGet("/reader/read/asyncOffTime");
            tags = read(readTime);
            for (TagReadData t : tags)
            {
              tagReadQueue.put(t);
            }
            if (sleepTime > 0)
            {
              Thread.sleep(sleepTime);
            }
          } 
          catch (ReaderException re)
          {
            exceptionQueue.put(re);
            enabled = false;
            running = false;
          }
        }
      } 
      catch (InterruptedException ie)
      {
        Thread.currentThread().interrupt();
        running = false;
        enabled = false;
      }
    }

    synchronized void readOn()
    {
      enabled = true;
      this.notifyAll();  // Notify change in enabled
    }

    synchronized void readOff()
    {
      enabled = false;
      try
      {
        while (running == true)
        {
          this.wait();  // Wait for running to change
        }
      }
      catch (InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
    }

  }

  /** 
   * Register a listener to be notified of message packets.
   *
   * @param listener the TransportListener to add
   */
  public abstract void addTransportListener(TransportListener listener);

  /** 
   * Remove a listener from the list of listeners to be notified of
   * message packets.
   *
   * @param listener the TransportListener to add
   */
  public abstract void removeTransportListener(TransportListener listener);

}
