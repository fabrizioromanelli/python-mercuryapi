#ifndef _TMR_TAGOP_H
#define _TMR_TAGOP_H
/** 
 *  @file tmr_tagop.h
 *  @brief Mercury API - Tag Operations Interface
 *  @author Nathan Williams
 *  @date 1/8/2010
 */

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


/** The type of a tag operation structure */
typedef enum TMR_TagopType
{
  /** Gen2 memory read */
  TMR_TAGOP_GEN2_READDATA,
  /** Gen2 memory write */
  TMR_TAGOP_GEN2_WRITEDATA,
  /** Gen2 memory lock/unlock */
  TMR_TAGOP_GEN2_LOCK,
  /** Gen2 tag kill */
  TMR_TAGOP_GEN2_KILL,
  /** Gen2 tag block write */
  TMR_TAGOP_GEN2_BLOCKWRITE,
  /** Gen2 tag block permalock */
  TMR_TAGOP_GEN2_BLOCKPERMALOCK,

  /** ISO180006B memory read */
  TMR_TAGOP_ISO180006B_READDATA,
  /** ISO180006B memory write */
  TMR_TAGOP_ISO180006B_WRITEDATA,
  /** ISO180006B memory lock/unlock */
  TMR_TAGOP_ISO180006B_LOCK,
  /** ISO180006B tag kill */

  /** List of tag operations */
  TMR_TAGOP_LIST
} TMR_TagopType;

/** Parameters of a Gen2 memory read operation */
typedef struct TMR_Tagop_GEN2_ReadData
{
  /** Gen2 memory bank to read from */
  TMR_GEN2_Bank bank;
  /** Word address to start reading at */
  uint32_t wordAddress;
  /** Number of words to read */
  uint8_t len;
} TMR_Tagop_GEN2_ReadData;

/** Parameters of a Gen2 memory write operation */
typedef struct TMR_Tagop_GEN2_WriteData
{
  /** Gen2 memory bank to write to */
  TMR_GEN2_Bank bank;
  /** Word address to start writing at */
  uint32_t wordAddress;
  /** Data to write */
  TMR_uint16List data;
} TMR_Tagop_GEN2_WriteData;

/** Parameters of a Gen2 memory lock/unlock operation */
typedef struct TMR_Tagop_GEN2_Lock
{ 
  /** Bitmask indicating which lock bits to change */
  uint16_t mask;
  /** New values of each bit specified in the mask */
  uint16_t action;
} TMR_Tagop_GEN2_Lock;

/** Parameters of a Gen2 tag kill operation */
typedef struct TMR_Tagop_GEN2_Kill
{
  /** Kill password to use to kill the tag */
  TMR_GEN2_Password password;
} TMR_Tagop_GEN2_Kill;

/** Parameters of a Gen2 tag Block Write operation */
typedef struct TMR_Tagop_GEN2_BlockWrite
{
  /** Gen2 memory bank to write to */
  TMR_GEN2_Bank bank; 
  /**the word address to start writing to*/
  uint32_t wordPtr; 
  /**the length of the data to write in words*/
  uint32_t wordCount; 
  /**the data to write*/
  uint8_t* data; 
  /**gen2 tag access password*/
  uint32_t accessPassword;
}TMR_Tagop_GEN2_BlockWrite;

/** Parameters of a Gen2 tag Block PermaLock operation */
typedef struct TMR_Tagop_GEN2_BlockPermaLock
{
  /**read or lock?*/
  uint32_t readLock; 
  /** Gen2 memory bank to lock */
  TMR_GEN2_Bank bank; 
  /**the staring word address to lock*/
  uint32_t blockPtr;
  /**number of 16 blocks*/
  uint32_t blockRange;
  /**mask */
  uint16_t* mask;
  /**gen2 tag access password*/
  uint32_t accessPassword;
}TMR_Tagop_GEN2_BlockPermaLock;

/** Parameters of a Gen2 memory lock/unlock operation */
typedef struct TMR_Tagop_ISO180006B_Lock
{ 
  /** The memory address of the byte to lock */
  uint8_t address;
} TMR_Tagop_ISO180006B_Lock;

/** Parameters of an ISO180006B memory read operation */
typedef struct TMR_Tagop_ISO180006B_ReadData
{
  /** Word address to start reading at */
  uint32_t wordAddress;
  /** Number of words to read */
  uint8_t len;
} TMR_Tagop_ISO180006B_ReadData;

/** Parameters of an ISO180006B memory write operation */
typedef struct TMR_Tagop_ISO180006B_WriteData
{
  /** Word address to start writing at */
  uint32_t wordAddress;
  /** Data to write */
  TMR_uint16List data;
} TMR_Tagop_ISO180006B_WriteData;


/* Forward declaration for the benefit of TMR_Tagop_List */
typedef struct TMR_Tagop TMR_Tagop;

/** List of tag operations */
typedef struct TMR_Tagop_List
{
  /** Array of pointers to tag operations*/
  TMR_Tagop **list;
  /** Number of tag operations in list */
  uint16_t len;
} TMR_Tagop_List;

/** Tag operation data structure */
struct TMR_Tagop
{
  TMR_TagopType type;
  union
  {
    TMR_Tagop_GEN2_WriteData gen2_writeData;
    TMR_Tagop_GEN2_ReadData gen2_readData;
    TMR_Tagop_GEN2_Lock gen2_lock;
    TMR_Tagop_GEN2_Kill gen2_kill;
    TMR_Tagop_GEN2_BlockWrite gen2_blockWrite;
    TMR_Tagop_GEN2_BlockPermaLock gen2_blockPermaLock;
    TMR_Tagop_ISO180006B_Lock iso180006b_lock;
    TMR_Tagop_ISO180006B_WriteData iso180006b_writeData;
    TMR_Tagop_ISO180006B_ReadData iso180006b_readData;
    TMR_Tagop_List list;
  } u;
};

TMR_Status TMR_Tagop_init_GEN2_ReadData(TMR_Tagop *tagop, TMR_GEN2_Bank bank,
                                        uint32_t wordAddress, uint8_t len);
TMR_Status TMR_Tagop_init_GEN2_WriteData(TMR_Tagop *tagop, TMR_GEN2_Bank bank,
                                         uint32_t wordAddress,
                                         TMR_uint16List *data);
TMR_Status TMR_Tagop_init_GEN2_Lock(TMR_Tagop *tagop, uint16_t mask,
                                    uint16_t action);
TMR_Status TMR_Tagop_init_GEN2_Kill(TMR_Tagop *tagop,
                                    TMR_GEN2_Password killPassword);
TMR_Status
TMR_Tagop_init_GEN2_BlockWrite(TMR_Tagop *tagop, TMR_GEN2_Bank bank, uint32_t wordPtr,      uint32_t wordCount, uint8_t* data, uint32_t accessPassword);

TMR_Status
TMR_Tagop_init_GEN2_BlockPermaLock(TMR_Tagop *tagop, uint32_t readLock, TMR_GEN2_Bank bank, uint32_t blockPtr, uint32_t blockRange, uint16_t* mask, uint32_t accessPassword);

TMR_Status TMR_Tagop_init_ISO180006B_ReadData(TMR_Tagop *tagop, uint32_t wordAddress, uint8_t len);
TMR_Status TMR_Tagop_init_ISO180006B_WriteData(TMR_Tagop *tagop, uint32_t wordAddress, TMR_uint16List *data);
TMR_Status TMR_Tagop_init_ISO180006B_Lock(TMR_Tagop *tagop, uint8_t address);


#endif /* _TMR_TAGOP_H */
