/* ex: set tabstop=2 shiftwidth=2 expandtab cindent: */
#ifndef _TMR_GEN2_H
#define _TMR_GEN2_H
/** 
 *  @file tmr_gen2.h  
 *  @brief Mercury API - Gen2 tag information and interfaces
 *  @author Brian Fiegel
 *  @date 5/7/2009
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

#ifdef  __cplusplus
extern "C" {
#endif

/** Memory lock bits */
typedef enum TMR_GEN2_LockBits
{
  /** User memory bank lock permalock bit */
  TMR_GEN2_LOCK_BITS_USER_PERM    = (1 << 0),
  /** User memory bank lock bit */
  TMR_GEN2_LOCK_BITS_USER         = (1 << 1),
  /** TID memory bank lock permalock bit */
  TMR_GEN2_LOCK_BITS_TID_PERM     = (1 << 2),
  /** TID memory bank lock bit */
  TMR_GEN2_LOCK_BITS_TID          = (1 << 3),
  /** EPC memory bank lock permalock bit */
  TMR_GEN2_LOCK_BITS_EPC_PERM     = (1 << 4),
  /** EPC memory bank lock bit */
  TMR_GEN2_LOCK_BITS_EPC          = (1 << 5),
  /** Access password lock permalock bit */
  TMR_GEN2_LOCK_BITS_ACCESS_PERM  = (1 << 6),
  /** Access password lock bit */
  TMR_GEN2_LOCK_BITS_ACCESS       = (1 << 7),
  /** Kill password lock permalock bit */
  TMR_GEN2_LOCK_BITS_KILL_PERM    = (1 << 8),
  /** Kill password lock bit */
  TMR_GEN2_LOCK_BITS_KILL         = (1 << 9)
} TMR_GEN2_LockBits;

/**
 * The arguments to a TMR_lockTag() method for Gen2 tags. 
 */
typedef struct TMR_GEN2_LockAction
{
  /** The gen2 lock mask bits */
  uint16_t mask;
  /** The gen2 lock action bits */
  uint16_t action;
}TMR_GEN2_LockAction;

/**
 * @ingroup tagauth
 * A 32-bit password (access or kill) in the Gen2 protocol.
 */
typedef uint32_t TMR_GEN2_Password;

/** Gen2 memory banks */
typedef enum TMR_GEN2_Bank
{
  /** Reserved bank (kill and access passwords) */
  TMR_GEN2_BANK_RESERVED  = 0,
  /** EPC memory bank */
  TMR_GEN2_BANK_EPC       = 1,
  /** TID memory bank */
  TMR_GEN2_BANK_TID       = 2,
  /** User memory bank */
  TMR_GEN2_BANK_USER      = 3
} TMR_GEN2_Bank;

/**
 * A single selection operation in the Gen2 protocol.
 * @ingroup filter
 */
typedef struct TMR_GEN2_Select
{
  /** Whether to invert the selection (deselect tags that meet the comparison) */
  bool invert;
  /** The memory bank in which to compare the mask */
  TMR_GEN2_Bank bank;
  /** The location (in bits) at which t to begin comparing the mask */
  uint32_t bitPointer;
  /** The length (in bits) of the mask */
  uint16_t maskBitLength;
  /** The mask value to compare with the specified region of tag memory, MSB first */ 
  uint8_t *mask;
} TMR_GEN2_Select;

/** Gen2 session values */
typedef enum TMR_GEN2_Session
{
  TMR_GEN2_SESSION_MIN= 0x00,
  /** Session 0 */
  TMR_GEN2_SESSION_S0 = 0x00,
  /** Session 1 */
  TMR_GEN2_SESSION_S1 = 0x01,
  /** Session 2 */ 
  TMR_GEN2_SESSION_S2 = 0x02,
  /** Session 3 */ 
  TMR_GEN2_SESSION_S3 = 0x03,
  TMR_GEN2_SESSION_MAX     = TMR_GEN2_SESSION_S3,
  TMR_GEN2_SESSION_INVALID = TMR_GEN2_SESSION_MAX + 1
} TMR_GEN2_Session;

/** Gen2 divide ratio values */
typedef enum TMR_GEN2_DivideRatio
{
  /** DR of 8 */
  TMR_GEN2_DIVIDE_RATIO_8    = 0,
  /** DR of 64/3 */
  TMR_GEN2_DIVIDE_RATIO_64_3 = 1
} TMR_GEN2_DivideRatio;

/** Gen2 TrExt bit */
typedef enum TMR_GEN2_TrExt
{
  /** No pilot tone in tag response */
  TMR_GEN2_TR_EXT_NO_PILOT_TONE = 0,
  /** Pilot tone in tag response */
  TMR_GEN2_TR_EXT_PILOT_TONE    = 1
} TMR_GEN2_TrExt;

/** Gen2 target search algorithms */
typedef enum TMR_GEN2_Target
{
  TMR_GEN2_TARGET_MIN= 0,
  /** Search target A */
  TMR_GEN2_TARGET_A  = 0,
  /** Search target B */
  TMR_GEN2_TARGET_B  = 1,
  /** Search target A until exhausted, then search target B */
  TMR_GEN2_TARGET_AB = 2,
  /** Search target B until exhausted, then search target A */
  TMR_GEN2_TARGET_BA = 3,
  TMR_GEN2_TARGET_MAX     = TMR_GEN2_TARGET_BA,
  TMR_GEN2_TARGET_INVALID = TMR_GEN2_TARGET_MAX+1
}TMR_GEN2_Target;

/** Gen2 tag encoding modulation values */
typedef enum TMR_GEN2_TagEncoding
{
  /** FM0 **/
  TMR_GEN2_FM0 = 0,

  TMR_GEN2_MILLER_MIN = 1,
  /** M = 2 */
  TMR_GEN2_MILLER_M_2 = 1,
  /** M = 4 */
  TMR_GEN2_MILLER_M_4 = 2,
  /** M = 8 */
  TMR_GEN2_MILLER_M_8 = 3,
  TMR_GEN2_MILLER_MAX     = TMR_GEN2_MILLER_M_8,
  TMR_GEN2_MILLER_INVALID = TMR_GEN2_MILLER_MAX+1
}TMR_GEN2_TagEncoding;

/** Gen2 link frequencies */
typedef enum TMR_GEN2_LinkFrequency
{
  /** 250 kHz */
  TMR_GEN2_LINKFREQUENCY_250KHZ  = 250,
  /** 400 kHz */
  TMR_GEN2_LINKFREQUENCY_400KHZ  = 400,
  /** 40 kHz */
  TMR_GEN2_LINKFREQUENCY_40KHZ   = 40,
  /**640 kHz*/
  TMR_GEN2_LINKFREQUENCY_640KHZ   = 640,
  TMR_GEN2_LINKFREQUENCY_MAX     = 640,
  TMR_GEN2_LINKFREQUENCY_INVALID = TMR_GEN2_LINKFREQUENCY_MAX + 1,
} TMR_GEN2_LinkFrequency;

/** Gen2 Tari values */
typedef enum TMR_GEN2_Tari
{
  /** Tari of 25 microseconds */
  TMR_GEN2_TARI_25US    = 0,
  /** Tari of 12.5 microseconds */
  TMR_GEN2_TARI_12_5US  = 1,
  /** Tari of 6.25 microseconds */
  TMR_GEN2_TARI_6_25US  = 2,
  TMR_GEN2_TARI_MAX     = 2,
  TMR_GEN2_TARI_INVALID = TMR_GEN2_TARI_MAX + 1,
} TMR_GEN2_Tari;

/** Gen2 WriteMode */
typedef enum TMR_GEN2_WriteMode
{
  /** WORD ONLY */
  TMR_GEN2_WORD_ONLY    = 0,
  /** BLOCK ONLY */
  TMR_GEN2_BLOCK_ONLY    = 1,
  /** BLOCK FALLBACK */
  TMR_GEN2_BLOCK_FALLBACK    = 2,

} TMR_GEN2_WriteMode;




typedef struct TMR_GEN2_HibikiSystemInformation
{
  uint16_t infoFlags;     /* Indicates whether the banks are present and Custom Commands are implemented*/ 
  uint8_t reservedMemory; /* Indicates the size of this memory bank in words*/
  uint8_t epcMemory;      /* Indicates the size of this memory bank in words*/
  uint8_t tidMemory;      /* Indicates the size of this memory bank in words*/
  uint8_t userMemory;     /* Indicates the size of this memory bank in words*/
  uint8_t setAttenuate;   /**/
  uint16_t bankLock;      /* Indicates Lock state for this type of lock*/
  uint16_t blockReadLock; /* Indicates Lock state for this type of lock*/
  uint16_t blockRwLock;   /* Indicates Lock state for this type of lock*/
  uint16_t blockWriteLock;/* Indicates Lock state for this type of lock*/
} TMR_GEN2_HibikiSystemInformation;

/** Size allocated for storing PC data in TMR_GEN2_TagData */
#define TMR_GEN2_MAX_PC_BYTE_COUNT (6)
/**
 * Gen2-specific per-tag data
 */
typedef struct TMR_GEN2_TagData
{ 
  /** Length of the tag PC */
  uint8_t pcByteCount;
  /** Tag PC */
  uint8_t pc[TMR_GEN2_MAX_PC_BYTE_COUNT];
} TMR_GEN2_TagData;



#ifdef __cplusplus
}
#endif

#endif /*_TMR_GEN2_H*/
