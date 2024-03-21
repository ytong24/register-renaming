/** *************************************************************************************
 * File         : RegFile.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Chisel implementation of Register File
 * ************************************************************************************* */

package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

/**
 * Enumerates the possible states of a Register File Entry.
 * - FREE: Indicates the entry is available for allocation.
 * - ALLOC: Indicates the entry is allocated and in use.
 * - COMMIT: Indicates the data in the entry is committed and can be used by dependent instructions.
 * - DEAD: Indicates the entry is no longer needed and can be reclaimed.
 */
object RegFileEntryState extends ChiselEnum {
  val FREE, ALLOC, COMMIT, DEAD = Value
}

/**
 * Represents a single entry in a Register File.
 *
 * @param archBit The number of bits to represent architectural register identifiers.
 * @param ptagBit The number of bits to represent physical register tags (ptags).
 */
class RegFileEntry(archBit: Int, ptagBit: Int) extends Bundle {
  val regPtag = UInt((ptagBit + 1).W)
  val regArchId = UInt((archBit + 1).W)
  val prevSameArchId = UInt((archBit + 1).W)
  val regState = RegFileEntryState()
}

/**
 * A Register File module that manages a set of Register File Entries.
 *
 * @param archIdNum The total number of architectural registers.
 * @param ptagNum   The total number of physical tags managed by the Register File.
 */
class RegFile(archIdNum: Int, ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val index = Input(UInt(log2Ceil(ptagNum + 1).W))
    val writeEnable = Input(Bool())
    val writeValue = Input(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))
    val readValue = Output(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))
  })

  // Represents an invalid value for architectural ID, used for initialization.
  val REG_FILE_INVALID_VAL = archIdNum.U(log2Ceil(archIdNum + 1).W)

  // Initialize all entries in the register file.
  val regFileEntries = RegInit(VecInit((0 until ptagNum).map { i =>
    val entry = Wire(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))
    entry.regPtag := i.U
    entry.regArchId := REG_FILE_INVALID_VAL
    entry.prevSameArchId := REG_FILE_INVALID_VAL
    entry.regState := RegFileEntryState.FREE
    entry
  }))

  val readValueReg = Reg(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))

  readValueReg := regFileEntries(io.index)
  io.readValue := readValueReg

  when(io.writeEnable) {
    regFileEntries(io.index) := io.writeValue
  }
}
