package org.aiotrade.lib.dbfdriver

import java.io.DataOutput
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Calendar
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @see http://www.clicketyclick.dk/databases/xbase/format/dbf.html#DBF_STRUCT
 */
object DBFHeader {
  val DBASE_III: Byte = 0x03
  
  val headLengthFixed = (
    1 +
    3 +
    4 +
    2 +
    2 +
    2 +
    1 +
    1 +
    4 +
    4 +
    4 +
    1 +
    1 +
    2 +
    //32 * fields.length +
    1
  )
  
  @throws(classOf[IOException])
  def read(in: ByteBuffer) = {
    val header = new DBFHeader

    header.versionNumber = in.get                           /* 0 */
    header.year  = in.get                                   /* 1 */
    header.month = in.get                                   /* 2 */
    header.day   = in.get                                   /* 3 */
    header.numberOfRecords = Utils.readLittleEndianInt(in)  /* 4-7 */
    header.headerLength = Utils.readLittleEndianShort(in)   /* 8-9 */
    header.recordLength = Utils.readLittleEndianShort(in)   /* 10-11 */
    header.reserved1 = Utils.readLittleEndianShort(in)      /* 12-13 */
    header.incompleteTransaction = in.get                   /* 14 */
    header.encryptionFlag = in.get                          /* 15 */
    header.freeRecordThread = Utils.readLittleEndianInt(in) /* 16-19 */
    header.reserved2 = in.getInt                            /* 20-23 */
    header.reserved3 = in.getInt                            /* 24-27 */
    header.mdxFlag = in.get                                 /* 28 */
    header.languageDriver = in.get                          /* 29 */
    header.reserved4 = Utils.readLittleEndianShort(in)      /* 30-31 */

    val fields = new ArrayBuffer[DBFField]
    var field = DBFField.read(in) /* 32 each */
    while (field != null) {
      fields += field
      field = DBFField.read(in)
    }
    header.fields = fields.toArray

    header
  }
}

import DBFHeader._
class DBFHeader {
  /* DBF structure start here */
  var versionNumber: Byte = DBASE_III   // 0
  var year: Byte = _                    // 1
  var month: Byte = _                   // 2
  var day: Byte = _                     // 3
  var numberOfRecords: Int = _          // 4-7
  var headerLength: Short = _           // 8-9
  var recordLength: Short = _           // 10-11
  var reserved1: Short = _              // 12-13
  var incompleteTransaction: Byte = _   // 14
  var encryptionFlag: Byte = _          // 15
  var freeRecordThread: Int = _         // 16-19
  var reserved2: Int = _                // 20-23
  var reserved3: Int = _                // 24-27
  var mdxFlag: Byte = _                 // 28
  var languageDriver: Byte = _          // 29
  var reserved4: Short = _              // 30-31
  var fields: Array[DBFField] = _       // each 32 bytes
  var terminator: Byte = 0x0D           // n+1
  // var databaseContainer; /* 263 bytes */
  /* DBF structure ends here */

  @throws(classOf[IOException])
  def write(out: DataOutput) {
    out.writeByte(versionNumber)                                  /* 0 */

    val calendar = Calendar.getInstance
    out.writeByte((calendar.get(Calendar.YEAR) - 1900).toByte)    /* 1 */
    out.writeByte((calendar.get(Calendar.MONTH) + 1).toByte)      /* 2 */
    out.writeByte((calendar.get(Calendar.DAY_OF_MONTH)).toByte)   /* 3 */

    out.writeInt(Utils.littleEndian(numberOfRecords))             /* 4-7 */
    out.writeShort(Utils.littleEndian(findHeaderLength))          /* 8-9 */
    out.writeShort(Utils.littleEndian(findRecordLength))          /* 10-11 */
    out.writeShort(Utils.littleEndian(reserved1))                 /* 12-13 */
    out.writeByte(incompleteTransaction)                          /* 14 */
    out.writeByte(encryptionFlag)                                 /* 15 */
    out.writeInt(Utils.littleEndian(freeRecordThread))            /* 16-19 */
    out.writeInt(Utils.littleEndian(reserved2))                   /* 20-23 */
    out.writeInt(Utils.littleEndian(reserved3))                   /* 24-27 */
    out.writeByte(mdxFlag)                                        /* 28 */
    out.writeByte(languageDriver)                                 /* 29 */
    out.writeShort(Utils.littleEndian(reserved4))                 /* 30-31 */

    var i = 0
    while (i < fields.length) {
      fields(i).write(out)
      i += 1
    }

    out.writeByte(terminator)                                     /* n + 1 */
  }


  private def findHeaderLength: Short =
    (headLengthFixed + 32 * fields.length).toShort
 
  private def findRecordLength: Short = {
    var t_recordLength = 0
    var i = 0
    while (i < fields.length) {
      t_recordLength += fields(i).length
      i += 1
    }

    (t_recordLength + 1).toShort
  }
}
