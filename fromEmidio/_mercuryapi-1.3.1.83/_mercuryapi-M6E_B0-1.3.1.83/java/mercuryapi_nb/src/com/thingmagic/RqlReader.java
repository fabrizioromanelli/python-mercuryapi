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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


/**
 * The RqlReader class is an implementation of a Reader object that
 * communicates with a ThingMagic fixed RFID reader via the RQL
 * network protocol.
 * <p>
 * Instances of the RQL class are created with the Reader.create()
 * method with a "rql" URI or a generic "tmr" URI that references a
 * network device.
 */
public class RqlReader extends com.thingmagic.Reader
{
  final static int[] gpioBits = {0x04, 0x08, 0x10, 0x02, 0x20, 0x40, 0x80, 0x100};

  // Connection state and fixed values
  int maxAntennas;
  boolean antennas[];
  Set<TagProtocol> protocolSet;
  String host;
  int port;
  List<TransportListener> listeners;
  boolean hasListeners;
  Socket rqlSock;
  int transportTimeout;
  BufferedReader rqlIn;
  BufferedWriter rqlOut;
  boolean isAstra; // for workarounds
  int[] gpiList, gpoList;

  // Values affected by parameter operations
  int txPower;

  RqlReader(String host)
    throws ReaderException
  {
    this(host, 8080);
  }

  RqlReader(String host, int port)
    throws ReaderException
  {
    this.host = host;
    this.port = port;

    listeners = new Vector<TransportListener>();

    rqlSock = new Socket();
    transportTimeout = 10000;
    addParam("/reader/transportTimeout",
             Integer.class, transportTimeout, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 setSoTimeout((Integer)value);
                 transportTimeout = (Integer)value;
                 return transportTimeout;
               }
               public Object get(Object value)
               {
                 return transportTimeout;
               }
             });
  }

  public void connect()
    throws ReaderException
  {
    String version, model, serial;
    TagProtocol[] protocols;
    Setting s;
    SettingAction saCopy;
    int session;

    try
    {
      rqlSock.connect(new InetSocketAddress(host, port));
      rqlIn = new BufferedReader(
        new InputStreamReader(rqlSock.getInputStream()));
      rqlOut = new BufferedWriter(
        new OutputStreamWriter(rqlSock.getOutputStream()));
    }
    catch (java.net.UnknownHostException e)
    {
      throw new ReaderCommException(e.getMessage());
    }
    catch (IOException e)
    {
      throw new ReaderCommException(e.getMessage());
    }

    maxAntennas = Integer.parseInt(getField("reader_available_antennas",
                                            "params"));
    serial = getField("reader_serial", "params");
    version = getField("version", "settings");

    isAstra = false;
    if (version.charAt(0) == '4')
    {
      model = "Astra";
      isAstra = true;
    }
    else if (version.charAt(0) == '2')
    {
      model = "M5"; // XXX can we distinguish an M4?
    }
    else
    {
      model = "unknown";
    }

    if (isAstra)
    {
      // Workaround for Astra bug where RQL may have cached an
      // incorrect value of the protocol list. Conveniently, we know
      // what protocols Astra supports.
      protocols = new TagProtocol[] {TagProtocol.GEN2};
      protocolSet = EnumSet.of(TagProtocol.GEN2);
    }
    else
    {
      int pcount;
      String[] protocolIds;
      protocolIds = getField("supported_protocols", "settings").split(" ");

      pcount = 0;
      for (String id : protocolIds)
      {
        if (TagProtocol.getProtocol(id) != null)
        {
          pcount++;
        }
      }
      protocols = new TagProtocol[pcount];
      protocolSet = EnumSet.noneOf(TagProtocol.class);
      for (String id : protocolIds)
      {
        TagProtocol p;
        p = TagProtocol.getProtocol(id);
        if (p != null)
        {
          protocols[--pcount] = p;
          protocolSet.add(p);
        }
      }
    }
    

    int[] antennaPorts = new int[maxAntennas];
    for (int i = 0; i < maxAntennas; i++)
    {
      antennaPorts[i] = i + 1;
    }

    if (isAstra)
    {
      gpiList = new int[] {3, 4, 6, 7};
      gpoList = new int[] {0, 1, 2, 5};

    }
    else
    {
      gpiList = new int[] {3, 4};
      gpoList = new int[] {0, 1, 2, 5};
    }

    txPower = Integer.parseInt(getField("tx_power", "saved_settings"));

    addParam("/reader/version/model",
             String.class, model, false, null);


    addParam("/reader/version/hardware",
                String.class, "", false, null);

               
    addParam("/reader/version/software",
                String.class, version, false, null);


    saCopy = new SettingAction() 
    {
      public Object set(Object value)
      {
        return value;
      }
      public Object get(Object value)
      {
        return copyIntArray(value);
      }
    };

    addParam("/reader/antenna/portList",
                int[].class, antennaPorts, false, saCopy);

    addParam("/reader/gpio/inputList",
                int[].class, gpiList, false, saCopy);

    addParam("/reader/gpio/outputList",
                int[].class, gpoList, false, saCopy);

    addParam("/reader/version/supportedProtocols",
                TagProtocol[].class, protocols, false,
                    new SettingAction() 
                    {
                      public Object set(Object value)
                      {
                        return value;
                      }
                      public Object get(Object value)
                      {
                        TagProtocol[] protos = (TagProtocol[])value;
                        TagProtocol[] protosCopy;
                        protosCopy = new TagProtocol[protos.length];
                        System.arraycopy(protos, 0, protosCopy, 0, protos.length);
                        return protosCopy;
        
                      }
    });

    addParam("/reader/radio/powerMin",
                Integer.class, 0 , false, null);
    addParam("/reader/radio/powerMax",
                Integer.class, isAstra ? 3000 : 3250 , false, null);

    addParam("/reader/antenna/connectedPortList",
                int[].class, null, false,
                    new SettingAction()
                    {
                      public Object set(Object value)
                      {
                        return value;
                      }
                      public Object get(Object value)
                        throws ReaderException
                      {
                        // RQL doesn't have an antenna-detection
                        // mechanism. The reader knows, and will use
                        // those antennas if we don't specify
                        // anything, but the user can't do the same
                        // thing manually.
                        return new int[] {};
                      }
    });

    addParam("/reader/gen2/accessPassword",
             Gen2.Password.class, null, true, null);

    addParam("/reader/radio/readPower",
                Integer.class, txPower, true,
                    new SettingAction()
                    {
                      public Object set(Object value)
                        throws ReaderException
                      {
                        int power = (Integer)value;
                        if (power < (Integer)paramGet("/reader/radio/powerMin") ||
                            power > (Integer)paramGet("/reader/radio/powerMax"))
                        {
                          throw new IllegalArgumentException(
                            "Invalid power level " + value);
                        }
                        return value;
                      }
                      public Object get(Object value)
                      {
                        return value;
                      }
    });

    addParam("/reader/radio/writePower",
                Integer.class, txPower, true,
                    new SettingAction()
                    {
                      public Object set(Object value)
                        throws ReaderException
                      {
                        int power = (Integer)value;
                        if (power < (Integer)paramGet("/reader/radio/powerMin") ||
                            power > (Integer)paramGet("/reader/radio/powerMax"))
                        {
                          throw new IllegalArgumentException(
                            "Invalid power level " + value);
                        }
                        return value;
                      }
                      public Object get(Object value)
                      {
                        return value;
                      }
    });

    addParam("/reader/read/plan",
                ReadPlan.class, new SimpleReadPlan(), true, null);

    addParam("/reader/tagop/protocol",
                TagProtocol.class, TagProtocol.GEN2, true,
                    new SettingAction() 
                    {
                      public Object set(Object value)
                      {
                        TagProtocol p = (TagProtocol)value;
                        
                        if (!protocolSet.contains(p))
                          throw new IllegalArgumentException(
                            "Unsupported protocol " + p + ".");
                        return value;
                      }
                      public Object get(Object value)
                      {
                        return value;
                      }
    });

    addParam("/reader/tagop/antenna",
                Integer.class, 1, true,
                    new SettingAction() 
                    {
                      public Object set(Object value)
                      {
                        int a = (Integer)value;
                        if (a < 1 || a > maxAntennas)
                          throw new IllegalArgumentException(
                            "Invalid antenna " + a + ".");
                        return value;
                      }
                      public Object get(Object value)
                      {
                        return value;
                      }
    });

    addParam("/reader/gen2/session",
                Gen2.Session.class, null, true,
                    new SettingAction()
                    {
                      public Object set(Object value)
                        throws ReaderException
                      {
                        runQuery(String.format(
                                   "UPDATE params SET gen2Session='%d';",
                                   ((Gen2.Session)value).rep));
                        return value;
                      }
                      public Object get(Object value)
                        throws ReaderException
                      {
                        int s = Integer.parseInt(getField("gen2Session", "params"));
                        switch (s)
                        {
                          // -1 is Astra's way of saying "Use the module's value".
                          // That value is dependant on the user mode, so check it.
                        case -1: 
                          int mode = Integer.parseInt(getField("userMode", "params"));
                          if (mode == 3) // portal mode
                          {
                            return Gen2.Session.S1;
                          }
                          else
                          {
                            return Gen2.Session.S0; 
                          }
                        case 0: return Gen2.Session.S0;
                        case 1: return Gen2.Session.S1;
                        case 2: return Gen2.Session.S2;
                        case 3: return Gen2.Session.S3;
                        }
                        throw new ReaderParseException("Unknown Gen2 session value " + s);
                      }
    });

    try {
      getField("gen2InitQ", "params");
      addParam("/reader/gen2/q",
               Gen2.Q.class, null, true,
               new SettingAction()
               {
                 public Object set(Object value)
                   throws ReaderException
                 {
                   if (value instanceof Gen2.StaticQ)
                   { 
                     String qval = Integer.toString(
                       ((Gen2.StaticQ)value).initialQ);
                     setField("gen2InitQ", qval, "params");
                     setField("gen2MinQ", qval, "params");
                     setField("gen2MaxQ", qval, "params");
                   }
                   else
                   {
                     setField("gen2MinQ", "2", "params");
                     setField("gen2MaxQ", "6", "params");
                   }
                   return value;
                 }
                 public Object get(Object value)
                   throws ReaderException
                 {
                   int q = Integer.parseInt(
                     getField("gen2InitQ", "params"));
                   int min = Integer.parseInt(
                     getField("gen2MinQ", "params"));
                   int max = Integer.parseInt(
                     getField("gen2MaxQ", "params"));
                   if (q == min && q == max)
                     return new Gen2.StaticQ(q);
                   else
                     return new Gen2.DynamicQ();
                 }
               });
    }
    catch (ReaderException re)
    {
    }

    try {
      getField("gen2Target", "params");
      addParam("/reader/gen2/target",
               Gen2.Target.class, null, true,
               new SettingAction()
               {
                 public Object set(Object value)
                   throws ReaderException
                 {
                   Gen2.Target t = (Gen2.Target)value;
                   int val;
                   switch (t)
                   {
                   case A:
                     val = 2;
                     break;
                   case B:
                     val = 3;
                     break;
                   case AB:
                     val = 0;
                     break;
                   case BA:
                     val = 1;
                     break;
                   default:
                     throw new IllegalArgumentException("Unknown target enum "
                                                        + t);
                   }
                   setField("gen2Target", Integer.toString(val), "params");
                   return value;
                 }
                 public Object get(Object value)
                   throws ReaderException
                 {
                   int val = Integer.parseInt(
                     getField("gen2Target", "params"));
                   switch (val)
                   {
                     // -1 is Astra's way of saying "Use the module default".
                     // Take advantage of knowing that default.
                   case -1:
                     return Gen2.Target.A;
                   case 0:
                     return Gen2.Target.AB;
                   case 1:
                     return Gen2.Target.BA;
                   case 2:
                     return Gen2.Target.A;
                   case 3:
                     return Gen2.Target.B;
                   }
                   throw new ReaderParseException("Unknown target value " + 
                                                  val);
                 }
               });
    }
    catch (ReaderException re)
    {
    }

    // It's okay if this doesn't get set to anything other than NONE;
    // since the user doesn't need to set the region, the connection
    // shouldn't fail just because the region is unknown.
    Reader.Region region = Reader.Region.NONE;

    String regionName = getField("regionName", "params").toUpperCase();
    if (regionName.equals("US"))
    {
      region = Reader.Region.NA;
    }
    else if (regionName.equals("JP"))
    {
      region = Reader.Region.JP;
    }
    else if (regionName.equals("CN"))
    {
      region = Reader.Region.PRC;
    }
    else if (regionName.equals("IN"))
    {
      region = Reader.Region.IN;
    }
    else if (regionName.equals("KR"))
    {
      String regionVersion = getField("region_version", "params");
      if (regionVersion.equals("1") || regionVersion.equals(""))
      {
        region = Reader.Region.KR;
      }
      else if (regionVersion.equals("2"))
      {
        region = Reader.Region.KR2;
      }
    }
    else if (regionName.equals("EU"))
    {
      String regionVersion = getField("region_version", "params");
      if (regionVersion.equals("1") || regionVersion.equals(""))
      {
        region = Reader.Region.EU;
      }
      else if (regionVersion.equals("2"))
      {
        region = Reader.Region.EU2;
      }
      else if (regionVersion.equals("3"))
      {
        region = Reader.Region.EU3;
      }
    }

    addParam("/reader/region/supportedRegions",
             Reader.Region[].class, new Reader.Region[] {region}, true, null);

    addParam("/reader/region/id",
             Reader.Region.class, region, true,
             new SettingAction()
             {
               public Object get(Object value)
                 throws ReaderException
               {
                 return value;
               }
               public Object set(Object value)
                 throws ReaderException
               {
                 if (value != paramGet("/reader/region/id"))
                 {
                   throw new IllegalArgumentException("Region not supported");
                 }
                 return value;
               }
             });

    connected = true;
  }

  void setSoTimeout(int timeout)
  {
    try
    {
      rqlSock.setSoTimeout(timeout);
    }
    catch (java.net.SocketException se)
    {
      // this is a "shouldn't happen" - convert to a major error
      throw new java.io.IOError(se);
    }
  }


  String singulationString(TagFilter t)
  {
    String s;
    byte[] data;

    if (t == null)
      s = "";
    else
    {
      if (!(t instanceof TagData))
      {
        throw new IllegalArgumentException("RQL only supports singulation by EPC");
      }
      s = String.format("AND id=0x%s", ((TagData)t).epcString());
    }

    return s;
  }

  int[] copyIntArray(Object value)
  {
    int[] original = (int[])value;
    int[] copy = new int[original.length];
    System.arraycopy(original, 0, copy, 0, original.length);
    return copy;
  }

  public void addTransportListener(TransportListener l)
  {
    listeners.add(l);
    hasListeners = true;
  }


  public void removeTransportListener(TransportListener l)
  {
    listeners.remove(l);
    if (listeners.isEmpty())
      hasListeners = false;
  }

  void notifyListeners(String s, boolean tx)
  {
    byte[] msg = s.getBytes();
    for (TransportListener l : listeners)
    {
      l.message(tx, msg, 0);
    }
  }

  void notifyListeners(List<String> strings, boolean tx)
  {
    StringBuilder sb = new StringBuilder();

    for (String s : strings)
    {
      sb.append(s);
      sb.append("\n");
    }
    
    byte[] msg = sb.toString().getBytes();
    for (TransportListener l : listeners)
    {
      l.message(tx, msg, 0);
    }
  }


  String[] runQuery(String q)
    throws ReaderException
  {
    return runQuery(q, false);
  }

  synchronized String[] runQuery(String q, boolean permitEmptyResponse)
    throws ReaderException
  {
    List<String> lines;
    boolean done;

    lines = new Vector<String>();
    if (hasListeners)
    {
      notifyListeners(q, true);
    }
    try
    {
      rqlOut.write(q);
      rqlOut.write('\n');
      rqlOut.flush();

      done = false;
      while (done == false)
      {
        String l;

        l = rqlIn.readLine();

        if (l.regionMatches(true, 0, "Error", 0, 5))
        {
          rqlIn.readLine(); // eat the extra line so future parsing works
          throw new ReaderException(l);
        }
        if (l != null && (permitEmptyResponse || !l.equals("")))
        {
          lines.add(l);
          permitEmptyResponse = false;
        }
        else
        {
          done = true;
        }
      }
      return lines.toArray(new String[lines.size()]);
    }
    catch (IOException e)
    {
      throw new ReaderCommException(e.getMessage());
    }
    finally
    {
      if (hasListeners)
      {
        notifyListeners(lines, false);
      }
    }
  }

  String getTable(String field)
  {
    if (field.equals("uhf_power_centidbm") ||
        field.equals("tx_power") ||
        field.equals("hostname") ||
        field.equals("iface") ||
        field.equals("dhcpcd") ||
        field.equals("ip_address") ||
        field.equals("netmask") ||
        field.equals("gateway") ||
        field.equals("ntp_servers") ||
        field.equals("epc1_id_length") ||
        field.equals("primary_dns") ||
        field.equals("secondary_dns") ||
        field.equals("domain_name") ||
        field.equals("reader_description") ||
        field.equals("reader_role") ||
        field.equals("ant1_readpoint_descr") ||
        field.equals("ant2_readpoint_descr") ||
        field.equals("ant3_readpoint_descr") ||
        field.equals("ant4_readpoint_descr"))
    {
      return "saved_settings";
    }
    else
    {
      return "params";
    }
  }
  /**
   * Get a value from the underlying RQL/device configuration table.
   *
   * @param field the name of configuration field to look up
   * @return the value of the field as a string
   */
  public String cmdGetParam(String field)
    throws ReaderException
  {
    return getFieldInternal(field, getTable(field));
  }

  /**
   * Set a value in the underlying RQL/device configuration table.
   *
   * @param field the name of configuration field to set
   * @return the value to set as a string
   */
  public void cmdSetParam(String field, String value)
    throws ReaderException
  {
    setField(field, value, getTable(field));
  }

  // Possible "RQL injection attack" - don't make this public
  // without sanitizing params.
  String getFieldInternal(String field, String table)
    throws ReaderException
  {
    String[] ret;

    ret = runQuery("SELECT " + field + " from " + table + ";", true);

    return ret[0];
  }

  // For internal use - throw an exception on blank responses.
  // Nothing we're asking about has a legitimate reason to be blank.
  String getField(String field, String table)
    throws ReaderException
  {
    String ret;
    
    ret = getFieldInternal(field, table);
    if (ret.length() == 0)
    {
      throw new ReaderParseException("No field " + field + " in table " + 
                                     table);
    }
    return ret;
  }


  void setField(String field, String value, String table)
    throws ReaderException
  {
    runQuery(String.format("UPDATE %s SET %s='%s';",
                           table, field, value));
  }

  public void destroy()
  {
    try
    {
      rqlSock.close();
    }
    catch (java.io.IOException e)
    {
      // How can we fail to be closed?
    }

    rqlIn = null;
    rqlOut = null;

    connected = false;
  }

  void setTxPower(int power)
    throws ReaderException
  {
    // We cache the value we last set the power to. This could be
    // changed out from under us my, for example, a m4api call.
    if (power != txPower)
    {
      runQuery(String.format("UPDATE saved_settings SET tx_power='%d';",
                             power));
      txPower = power;
    }
  }

  String protocolStr(TagProtocol protocol)
  {
    switch (protocol)
    {
    case GEN2: return "GEN2";
    case ISO180006B: return "ISO18000-6B";
    case ISO180006B_UCODE: return "ISO18000-6B";
    }
    throw new IllegalArgumentException("RQL does not support protocol " + 
                                       protocol);
  }

  TagProtocol codeToProtocol(int code)
  {
    switch (code)
    {
    case 8: return TagProtocol.ISO180006B;
    case 12: return TagProtocol.GEN2;
    }
    return null;
  }

  String readPlanWhereClause(ReadPlan rp)
  {
    String clause;

    if (rp instanceof SimpleReadPlan)
    {
      SimpleReadPlan srp = (SimpleReadPlan)rp;
      StringBuilder sb = new StringBuilder();

      sb.append(String.format("protocol_id='%s'",
                              protocolStr(srp.protocol)));
      if (srp.antennas.length > 0)
      {
        sb.append(" AND (");
        sb.append(String.format("antenna_id=%d", srp.antennas[0]));
        for (int i = 1; i < srp.antennas.length; i++)
        {
          sb.append(String.format(" OR antenna_id=%d", srp.antennas[i]));
        }
        sb.append(")");
      }
      clause = sb.toString();
    }
    else
    {
      throw new RuntimeException("Unknown ReadPlan passed to readPlanWhereClause" + rp.getClass());
    }
    return clause;
  }

  void setUcodeMode(SimpleReadPlan srp)
    throws ReaderException
  {
    if (srp.protocol == TagProtocol.ISO180006B)
    {
      setField("useUCodeEpc", "no", "params");
    }
    else if (srp.protocol == TagProtocol.ISO180006B_UCODE)
    {
      setField("useUCodeEpc", "yes", "params");
    }
  }
  
  public TagReadData[] read(long duration)
    throws ReaderException
  {
    List<TagReadData> tagvec;

    setTxPower((Integer)paramGet("/reader/radio/readPower"));
    tagvec = new Vector<TagReadData>();

    try
    {
      setSoTimeout((int)duration + transportTimeout);
      readInternal(duration, (ReadPlan)paramGet("/reader/read/plan"), tagvec);
    }
    finally
    {
      setSoTimeout(transportTimeout);
    }
    return tagvec.toArray(new TagReadData[tagvec.size()]);
  }

  public void readInternal(long duration, ReadPlan rp,
                           List<TagReadData> tagvec)
    throws ReaderException
  {
    Date startTime;
    long startTimeMillis;
    String query;
    String[] lines;
    int numTags;
    String where, where1, where2;

    if (rp instanceof MultiReadPlan)
    {
      MultiReadPlan mrp = (MultiReadPlan)rp;
      for (ReadPlan r : mrp.plans)
      {
        readInternal(duration * r.weight / mrp.totalWeight, r, tagvec);
      }
      return;
    }

    SimpleReadPlan sp = (SimpleReadPlan)rp;
    setUcodeMode(sp);

    where1 = readPlanWhereClause(sp);
    where2 = singulationString(sp.filter);
    if (where1.equals("") && where2.equals(""))
    {
      where = "";
    }
    else
    {
      where = " WHERE ";
    }
    query = String.format("SELECT protocol_id,antenna_id,read_count,id"
                          + ",frequency,dspmicros"
                          + "%s"
                          + " FROM tag_id"
                          + "%s%s%s"
                          + " set time_out=%d;", 
                          isAstra ? ",lqi" : "",
                          where, where1, where2,
                          duration);

    startTime = new Date();
    lines = runQuery(query);
    numTags = lines.length;
    startTimeMillis = startTime.getTime();

    for (int i = 0; i < numTags; i++)
    {
      String l, epc, crc;
      String fields[];
      TagProtocol proto;
      TagReadData t;
      TagData tag;
      int f;

      l = lines[i];
      fields = l.split("\\|");

      proto = codeToProtocol(Integer.parseInt(fields[0]));
      switch (proto)
      {
      case GEN2:
        epc = fields[3].substring(2, fields[3].length() - 4);
        crc = fields[3].substring(fields[3].length() - 4);
        tag        = new Gen2.TagData(epc, crc);
        break;
      case ISO180006B:
        epc = fields[3].substring(2, fields[3].length() - 4);
        crc = fields[3].substring(fields[3].length() - 4);
        tag        = new Iso180006b.TagData(epc, crc);
        break;
      default:
        throw new ReaderParseException("Unknown protocol code " + fields[0]);
      }
      t = new TagReadData();
      t.tag        = tag;
      t.antenna    = Integer.parseInt(fields[1]);
      t.readCount  = Integer.parseInt(fields[2]);
      t.frequency  = Integer.parseInt(fields[4]);
      t.readOffset = Integer.parseInt(fields[5]) / 1000;
      if (isAstra)
      {
        t.rssi      = Integer.parseInt(fields[6]);
      }
      else
      {
        t.rssi      = 0;
      }
      t.readBase   = startTimeMillis;
      tagvec.add(t);
    }
  }

  String tagopWhereClause(TagFilter target)
  {
    int antenna;
    TagProtocol protocol;

    try
    {
      antenna = (Integer)paramGet("/reader/tagop/antenna");
      protocol = (TagProtocol)paramGet("/reader/tagop/protocol");
    }
    catch (ReaderException re)
    {
      // If these parameters aren't set, something very bad has happened
      // to the internal state of this module; fail.
      throw new AssertionError("tagop parameters not set");
    }

    return String.format("protocol_id='%s'"
                         + " AND antenna_id=%d"
                         + " %s",
                         protocolStr(protocol), antenna,
                         singulationString(target));
  }


  String tagopSetClause()
    throws ReaderException
  {
    Gen2.Password password;

    password = (Gen2.Password)paramGet("/reader/gen2/accessPassword");
    if (password == null || password.password == 0)
    {
      return "";
    }
    
    return String.format(",password=0x%x", password.password);
  }

  public void writeTag(TagFilter oldID, TagData newID)
    throws ReaderException
  {
    String query;

    setTxPower((Integer)paramGet("/reader/radio/writePower"));

    if (isAstra)
    {
      // The regular UPDATE command below won't work on current Astra
      // with respect to singulation. Outsource this work to the
      // memory-write.
      writeTagMemBytes(oldID,
                       1, // EPC bank
                       4, // word address 2 - skip PC and CRC
                       newID.epcBytes());
    }
    else
    {
      query = String.format("UPDATE tag_id"
                            + " SET id=0x%s"
                            + " %s"
                            + " WHERE %s;",
                            newID.epcString(),
                            tagopSetClause(),
                            tagopWhereClause(oldID));
      runQuery(query);
    }
  }

  public void killTag(TagFilter target, TagAuthentication auth)
    throws ReaderException
  {
    String query;
    int killPassword;

    if (auth == null)
    {
      killPassword = 0;
    }
    else if (auth instanceof Gen2.Password)
    {
      killPassword = ((Gen2.Password)auth).password;
    }
    else
    {
      throw new IllegalArgumentException("Unknown kill authentication type.");
    }

    query = String.format("UPDATE tag_id"
                          + " SET killed=1,password=0x%x"
                          + " WHERE %s;",
                          killPassword,
                          tagopWhereClause(target));
    runQuery(query);
  }

  void checkMemParams(int bank, int address, int count)
  {
    if (bank < 0 || bank > 3)
    {
      throw new IllegalArgumentException("Invalid memory bank " + bank);
    }
    if (count < 0 || count > 8)
    {
      throw new IllegalArgumentException("Invalid word count " + count
                                         + " (out of range)");
    }
  }

  public byte[] readTagMemBytes(TagFilter target,
                                int bank, int address, int count)
    throws ReaderException
  {
    String query;
    String[] ret;
    byte[] bytes;
    int wordAddress, wordCount;
    int start;

    wordAddress = address / 2;
    wordCount = (count + 1 + (address % 2) ) / 2;

    checkMemParams(bank, wordAddress, wordCount);

    setTxPower((Integer)paramGet("/reader/radio/readPower"));

    query = String.format("SELECT data FROM tag_data"
                          + " WHERE block_number=%d"
                          + " AND block_count=%d"
                          + " AND mem_bank=%d"
                          + " AND %s;",
                          wordAddress, wordCount, bank,
                          tagopWhereClause(target));

    ret = runQuery(query);
    
    start = 2;
    if (wordAddress * 2 != address)
    {
      start = 4;
    }
    bytes = new byte[count];
    for (int i = 0; i < count; i++)
    {
      String byteStr = ret[0].substring(start + i*2, start + 2 + i*2);
      bytes[i] = (byte)Integer.parseInt(byteStr, 16);
    }
    return bytes;
  }

  public short[] readTagMemWords(TagFilter target,
                                 int bank, int address, int count)
    throws ReaderException
  {
    String query;
    String[] ret;
    short[] words;

    checkMemParams(bank, address, count);

    setTxPower((Integer)paramGet("/reader/radio/readPower"));

    query = String.format("SELECT data FROM tag_data"
                          + " WHERE block_number=%d"
                          + " AND block_count=%d"
                          + " AND mem_bank=%d"
                          + " AND %s;",
                          address, count, bank,
                          tagopWhereClause(target));

    ret = runQuery(query);

    words = new short[count];
    for (int i = 0; i < count; i++)
    {
      String wordStr = ret[0].substring(2 + i*4, 6 + i*4);
      words[i] = (short)Integer.parseInt(wordStr, 16);
    }
    return words;
  }

  public void writeTagMemBytes(TagFilter target,
                               int bank, int address, byte[] data)
    throws ReaderException
  {
    int wordAddress;
    StringBuilder dataStr;
    String query;

    if ((address % 2) != 0)
    {
      throw new IllegalArgumentException("Byte write address must be even");
    }
    if ((data.length % 2) != 0)
    {
      throw new IllegalArgumentException("Byte write length must be even");
    }

    wordAddress = address / 2;

    checkMemParams(bank, wordAddress, data.length / 2);

    setTxPower((Integer)paramGet("/reader/radio/writePower"));

    dataStr = new StringBuilder();
    for (byte b : data)
    {
      dataStr.append(String.format("%02x", b));
    }

    query = String.format("UPDATE tag_data"
                          + " SET data=0x%s"
                          + " %s"
                          + " WHERE block_number=%d"
                          + " AND mem_bank=%d"
                          + " AND %s;",
                          dataStr,
                          tagopSetClause(),
                          wordAddress, bank,
                          tagopWhereClause(target));

    runQuery(query);
  }

  public void writeTagMemWords(TagFilter target,
                               int bank, int address, short[] data)
    throws ReaderException
  {
    StringBuilder dataStr;
    String query;

    checkMemParams(bank, address, data.length);

    setTxPower((Integer)paramGet("/reader/radio/writePower"));

    dataStr = new StringBuilder();
    for (short s : data)
    {
      dataStr.append(String.format("%04x", s));
    }

    query = String.format("UPDATE tag_data"
                          + " SET data=0x%s"
                          + " %s"
                          + " WHERE block_number=%d"
                          + " AND mem_bank=%d"
                          + " AND %s;",
                          dataStr,
                          tagopSetClause(),
                          address, bank,
                          tagopWhereClause(target));

    runQuery(query);
  }

  public void lockTag(TagFilter target, TagLockAction lock)
    throws ReaderException
  {
    String query;

    if (lock instanceof Gen2.LockAction)
    {
      Gen2.LockAction la = (Gen2.LockAction) lock;

      // RQL splits the Gen2 memory bank lock bits into "ID" and
      // "Data" pieces, such that you can't update both at once.
      if ((la.mask & 0x3FC) != 0)
      {
        query = String.format("UPDATE tag_id"
                              + " SET locked=%d"
                              + " %s"
                              + " WHERE %s AND type=%d;",
                              la.action,
                              tagopSetClause(),
                              tagopWhereClause(target),
                              la.mask);
        runQuery(query);
      }
      if ((la.mask & 0x3) != 0)
      {
        query = String.format("UPDATE tag_data"
                              + " SET locked=%d"
                              + " %s"
                              + " WHERE %s AND type=%d;",
                              la.action,
                              tagopSetClause(),
                              tagopWhereClause(target),
                              la.mask);
        runQuery(query);
      }
    }
  }

  public synchronized void firmwareLoad(InputStream fwStr)
    throws IOException, ReaderException
  {
    URL u;
    HttpURLConnection uc;
    String encoding;
    ClientHttpRequest c;
    InputStream replyStream;
    BufferedReader replyReader;
    StringBuilder replyBuf;
    String reply;
    char buf[] = new char[1024];
    int len;

    // These are about to stop working
    rqlSock.close();
    rqlSock = null;
    rqlIn = null;
    rqlOut = null;
    
    // Assume that a system with an RQL interpreter has the standard
    // web interface and password. This isn't really an RQL operation,
    // but it will work most of the time.

    u = new URL("http", host, 80, "/cgi-bin/firmware.cgi");
    uc = (HttpURLConnection)u.openConnection();

    encoding = "d2ViOnJhZGlv"; // base64 encoding of "web:radio"
    uc.setRequestProperty("Authorization", "Basic " + encoding);

    c = new ClientHttpRequest(uc);

    // "firmware.tmfw" is arbitrary
    c.setParameter("uploadfile", "firmware.tmfw", fwStr);

    replyStream = c.post();
    replyReader = new BufferedReader(new InputStreamReader(replyStream));
    replyBuf = new StringBuilder();

    do
    {
      len = replyReader.read(buf, 0, 1024);
      if (len > 0)
      {
        replyBuf.append(buf);
      }
    } while (len >= 0);

    replyStream.close();
    reply = replyBuf.toString();

    if (reply.indexOf("replace the new firmware with older firmware") != -1)
    {
      // We've been asked to confirm using an older firmware
      u = new URL("http", host, 80,
                  "/cgi-bin/firmware.cgi?confirm=true&DOWNGRADE=Continue");
      uc = (HttpURLConnection)u.openConnection();
      uc.setRequestProperty("Authorization", "Basic " + encoding);
      uc.setRequestMethod("GET");
      uc.connect();

      replyStream = uc.getInputStream();
      replyReader = new BufferedReader(new InputStreamReader(replyStream));
      replyBuf = new StringBuilder();

      do
      {
        len = replyReader.read(buf, 0, 1024);
        if (len > 0)
        {
          replyBuf.append(buf);
        }
      } while (len >= 0);

      replyStream.close();
      reply = replyBuf.toString();
    }

    if (reply.indexOf("Firmware update complete") != -1)
    {
      // Restart reader
      u = new URL("http", host, 80, "/cgi-bin/reset.cgi");
      uc = (HttpURLConnection)u.openConnection();
      uc.setRequestProperty("Authorization", "Basic " + encoding);
      c = new ClientHttpRequest(uc);
      c.setParameter("dummy", "dummy");
      c.post();

      // Wait for reader to come back up.
      try
      {
        Thread.sleep(90 * 1000);
      }
      catch(InterruptedException ie)
      {
      }
    }
    else
    {
      throw new ReaderException("Firmware update failed");
    }

    // Reconnect to the reader
    rqlSock = new Socket();
    setSoTimeout(transportTimeout);
    connect();
  }

  public GpioPin[] gpiGet()
    throws ReaderException
  {
    String[] ret;
    int gpioVal;
    GpioPin[] state;

    ret = runQuery("SELECT data FROM io WHERE mask=0xffffffff;");
    gpioVal = Integer.decode(ret[0]);

    state = new GpioPin[gpiList.length];
    for (int i = 0; i < gpiList.length; i++)
    {
      state[i] = new GpioPin(gpiList[i], ((gpioVal & gpioBits[gpiList[i]]) != 0));
    }

    return state;
  }

  public void gpoSet(GpioPin[] state)
    throws ReaderException
  {
    String query;
    int mask, data;

    mask = 0;
    data = 0;
    for (GpioPin gp : state)
    {
      mask |= gpioBits[gp.id];
      if (gp.high)
      {
        data |= gpioBits[gp.id];
      }
    }

    query = String.format("UPDATE io SET data=0x%x WHERE mask=0x%x;",
                          data, mask);

    runQuery(query);
  }

}
