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

import java.nio.ByteBuffer;

public class LlrpMessage
{
  ByteBuffer msgb;
  boolean lengthSet;
  protected static final int headerOffset = 10;

  public static final int
    MSG_CLOSE_CONNECTION_RESPONSE = 4,
    MSG_CLOSE_CONNECTION = 14,
    MSG_READER_EVENT_NOTIFICATION = 63,
    MSG_CUSTOM_MESSAGE = 1023;

  public static final int
    PARAM_READER_EVENT_NOTIFICATION_DATA = 246,
    PARAM_CONNECTION_ATTEMPT_EVENT = 256,
    PARAM_CONNECTION_CLOSE_EVENT = 257;
  
  public LlrpMessage(int type, int id)
  {
    msgb = ByteBuffer.allocate(270);
    msgb.putShort((short)(0x0400 | (type & 0x3ff)));
    msgb.putInt(0); // length; will be fixed up later
    lengthSet = false;
    msgb.putInt(id);
  }

  public LlrpMessage(ByteBuffer data)
  {
    msgb = data.duplicate().asReadOnlyBuffer();
    lengthSet = true;
  }

  public int getMessageType()
  {
    return msgb.getShort(0) & 0x3ff;
  }
  
  public int getMessageLength()
  {
    return msgb.position();
  }

  public int getMessageId()
  {
    return msgb.getInt(6);
  }

  public ByteBuffer transportBuffer()
  {
    if (lengthSet == false)
    {
      msgb.putInt(2, msgb.position());
      lengthSet = true;
    }
    ByteBuffer b = (ByteBuffer)msgb.asReadOnlyBuffer().flip();
    return b;

  }
  
  // Array containing the length of each TV-type parameter
  // (0-127), or -1 if there is no such parameter
  static final int tvParamLengths[] = new int[] {
  // 0   1   2   3   4   5   6   7   8   9
    -1,  3,  8,  8,  8, -1,  3,  3,  3,  4, // 0-9
     3,  3,  3, 12,  3,  3,  4,  3,  4, -1, // 10-19
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 20-29
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 30-39
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 40-49
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 50-59
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 60-69
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 70-79
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 80-89
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 90-99
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 100-109
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 110-119
    -1, -1, -1, -1, -1, -1, -1, -1}; // 120-127

  public ByteBuffer getParameter(int paramType)
    throws LlrpException
  {
    return getParameterInternal(msgb, headerOffset, paramType);
  }

  public static ByteBuffer getSubParameter(ByteBuffer param, int paramType)
    throws LlrpException
  {
    return getParameterInternal(param, 0, paramType);
  }

  static ByteBuffer getParameterInternal(ByteBuffer param, int offset, 
                                         int paramType)
    throws LlrpException
  {
    // Parameters can be in any order, so we have to do this scan
    // every time; thus, fetching N paramaters is a N^2 operation. A
    // Map of param types/offsets could help if this turns out to be a
    // problem.
    while (offset < param.limit())
    {
      int typefield = param.getShort(offset);
      int type, length;
      int headersize = 2;
      if ((typefield & 0x8000) != 0)
      {
        // type-value parameter. Length is implicit in the type.
        type = (typefield >> 8) & 0x7f;
        length = tvParamLengths[type];
        if (length == -1)
        {
          throw new LlrpException(null,
              "Unknown length for TV parameter " + type);
        }
      }
      else
      {
        // type-length-value parameter.
        type = typefield & 0x3ff;
        length = param.getShort(offset + 2);
        headersize += 2;
      }

      if (type == paramType)
      {
        int x = param.position();
        param.position(offset + headersize);
        ByteBuffer value = param.slice();
        param.position(x);
        value.limit(length - headersize);
        return value;
      }
      offset += length;
    }
    return null;
  }

  static LlrpMessage fromBuffer(ByteBuffer buf)
  {
    switch (buf.getShort(0) & 0x3ff)
    {
    case MSG_CUSTOM_MESSAGE:
      return new LlrpCustomMessage(buf);
    default:
      return new LlrpMessage(buf);
    }
  }
}
