/*
 * Copyright (c) 2009 ThingMagic, Inc.
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

import com.thingmagic.llrp.*;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

public class LlrpTransport implements SerialTransport
{
  String host;
  int port;
  SocketChannel sock;
  LlrpConnection lc;
  int nextId;

  public LlrpTransport(String host, int port)
  {
    this.host = host;
    this.port = port;
  }

  public void open()
    throws ReaderException
  {
    try
    {
      sock = SocketChannel.open(new InetSocketAddress(host, port));
      lc = new LlrpConnection(sock);

      byte[] message = new byte[2];

      message[0] = 0;
      message[1] = 1; // control indicator 0x0001 - power cycle module
      LlrpMessage lm = new LlrpCustomMessage(0x67ba, 3, message, 2, nextId++);
      lc.sendMessage(lm);
      lc.receiveMessage();
      // check for success
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
    catch (TimeoutException te)
    {
      throw new ReaderCommException("Timeout");
    }
    catch (LlrpException le)
    {
      throw new ReaderCommException("LLRP error: " + le.getMessage());
    }
  }

  public void sendBytes(int length, byte[] message, int offset, int timeoutMs)
    throws ReaderException
  {
    try
    {
      lc.sendMessage(new LlrpCustomMessage(0x67ba, 1, message, length,
                                           nextId++));
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
  }

  public byte[] receiveBytes(int length, byte[] messageSpace, int offset,
                             int timeoutMillis)
    throws ReaderException
  {
    if (messageSpace == null)
    {
      messageSpace = new byte[255];
    }

    try
    {
      sock.socket().setSoTimeout(timeoutMillis);

      LlrpMessage lm = lc.receiveMessage();
      while (!(lm instanceof LlrpCustomMessage))
      {
        // Notifications of connection attempts can happen at any point.
        if (lm.getMessageType() == LlrpMessage.MSG_READER_EVENT_NOTIFICATION)
        {
          ByteBuffer data = lm.getParameter(
            LlrpMessage.PARAM_READER_EVENT_NOTIFICATION_DATA);
          if (data == null)
          {
            throw new ReaderCommException("LLRP error: " 
                                          +"No reader event notification data");
          }
          ByteBuffer event = 
            LlrpMessage.getSubParameter(data,
                                        LlrpMessage.PARAM_CONNECTION_ATTEMPT_EVENT);
          if (event == null)
          {
            throw new ReaderCommException("No connection attempt event parameter");
          }
          int status = event.getShort();        
          
          if (status == 4)
          {
            lm = lc.receiveMessage();
            continue;
          }
        }
        throw new ReaderCommException(
          "Recieved unexpected LLRP message type " + lm.getMessageType());
      }
      LlrpCustomMessage lcm = (LlrpCustomMessage)lm;
      ByteBuffer payload = lcm.getPayload();
      payload.get(messageSpace, offset, payload.remaining());
      return messageSpace;
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
    catch (TimeoutException te)
    {
      throw new ReaderCommException("Timeout");
    }
    catch (LlrpException le)
    {
      throw new ReaderCommException("LLRP error: " + le.getMessage());
    }
  }

  public int getBaudRate()
    throws ReaderException
  {
    throw new UnsupportedOperationException();
  }

  public void setBaudRate(int rate)
    throws ReaderException
  {
    byte[] message = new byte[6];

    message[0] = 0;
    message[1] = 2;
    message[2] = (byte)((rate >> 24) & 0xff);
    message[3] = (byte)((rate >> 16) & 0xff);
    message[4] = (byte)((rate >>  8) & 0xff);
    message[5] = (byte)((rate >>  0) & 0xff);
    LlrpMessage lm = new LlrpCustomMessage(0x67ba, 3, message, 6, nextId++);
    try
    {
      lc.sendMessage(lm);
      lc.receiveMessage(); // don't really care
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
    catch (TimeoutException te)
    {
      throw new ReaderCommException("Timeout");
    }
  }

  public void powerCycle()
    throws ReaderException
  {
    byte[] message = new byte[2];

    message[0] = 0;
    message[1] = 1;
    LlrpMessage lm = new LlrpCustomMessage(0x67ba, 3, message, 2, nextId++);
    try
    {
      lc.sendMessage(lm);
      lc.receiveMessage(); // don't really care
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
    catch (TimeoutException te)
    {
      throw new ReaderCommException("Timeout");
    }

  }

  public void gpioSet(int mask, int value)
    throws ReaderException
  {
    byte[] message = new byte[4];

    message[0] = 0;
    message[1] = 3;
    message[2] = (byte)mask;
    message[3] = (byte)value;

    LlrpMessage lm = new LlrpCustomMessage(0x67ba, 3, message, 4, nextId++);
    try
    {
      lc.sendMessage(lm);
      lc.receiveMessage(); // don't really care
    }
    catch (IOException ioe)
    {
      throw new ReaderCommException("IO error: " + ioe.getMessage());
    }
    catch (TimeoutException te)
    {
      throw new ReaderCommException("Timeout");
    }

  }

  public void flush()
  {
  }

  public void shutdown()
    throws ReaderException
  {
    try
    {
      lc.shutdown();
      sock.close();
    }
    // Whatever, we're shutting down.
    catch (IOException ioe)
    {
    }
    catch (TimeoutException te)
    {
    }
  }


}
