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

public class LlrpCustomMessage extends LlrpMessage
{
  protected static final int headerOffset = 
    LlrpMessage.headerOffset + 5; // int + byte

  LlrpCustomMessage(ByteBuffer data)
  {
    super(data);
  }

  LlrpCustomMessage(int vendor, int subtype, int id)
  {
    super(MSG_CUSTOM_MESSAGE, id);
    msgb.putInt(vendor);
    msgb.put((byte)subtype);
  }

  public LlrpCustomMessage(int vendor, int subtype, byte[] payload, int length, 
                           int id)
  {
    super(MSG_CUSTOM_MESSAGE, id);
    msgb.putInt(vendor);
    msgb.put((byte)subtype);
    msgb.put(payload, 0, length);
  }

  int getVendorId()
  {
    return msgb.getInt(10);
  }

  int getMessageSubtype()
  {
    return msgb.get(14);
  }
 
  public ByteBuffer getPayload()
  {
    ByteBuffer payload = msgb.asReadOnlyBuffer();
    payload.position(headerOffset);
    return payload;
  }
}

