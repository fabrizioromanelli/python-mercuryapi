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
package com.thingmagic.llrp;

import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.TimeoutException;

public class LlrpConnection
{
  ByteChannel channel;

  public LlrpConnection(ByteChannel channel)
    throws IOException, TimeoutException, LlrpException
  {
    this.channel = channel;
    LlrpMessage event = receiveMessage();
    if (event.getMessageType() != LlrpMessage.MSG_READER_EVENT_NOTIFICATION)
    {
      throw new LlrpException(event, "Wrong message type");
    }
    ByteBuffer param = event.getParameter(
      LlrpMessage.PARAM_READER_EVENT_NOTIFICATION_DATA);
    if (param == null)
    {
      throw new LlrpException(event, "No reader event notification data");
    }
    param = LlrpMessage.getSubParameter(param, 
                                        LlrpMessage.PARAM_CONNECTION_ATTEMPT_EVENT);
    if (param == null)
    {
      throw new LlrpException(event, "No connection attempt event parameter");
    }
    int status = param.getShort();
    switch (status)
    {
    case 0:
      break; // success
    case 1:
      throw new LlrpException(event, "Connection failed: "
                              + "Reader-initiated connection already exists.");
    case 2:
      throw new LlrpException(event, "Connection failed: "
                              + "Client-initiated connection already exists.");
    case 3:
      throw new LlrpException(event, "Connection failed: Unknown reason");
    default:
      throw new LlrpException(event, "Unknown connection attempt event code "
                              + status);
    }
  }

  public void sendMessage(LlrpMessage message)
    throws IOException
  {
    channel.write(message.transportBuffer());
  }

  public LlrpMessage receiveMessage()
    throws IOException, TimeoutException
  {
    LlrpMessage response;
    ByteBuffer buf = ByteBuffer.allocate(270);
    try
    {
      do
      {
        int len = channel.read(buf);
        if (len  < 0)
        {
          throw new EOFException("Didn't expect end of data"); // XXX
        }
      } while (buf.position() < 10 || buf.position() < buf.getInt(2));
    }
    catch (java.net.SocketException se)
    {
      throw new TimeoutException("Timeout reading from socket");
    }
    buf.flip();
    
    return LlrpMessage.fromBuffer(buf);
  }

  public void shutdown()
    throws IOException, TimeoutException
  {
    LlrpMessage closeConnection =
      new LlrpMessage(LlrpMessage.MSG_CLOSE_CONNECTION, 0xffff);
    sendMessage(closeConnection);
    LlrpMessage closeConnectionResponse = receiveMessage();
  }
}
