/** *************************************************************************************
 * File         : RegMap.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Chisel implementation of Register Map
 * ************************************************************************************* */

package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

/**
 * A Register Map module for tracking the mapping between architectural
 * register identifiers and physical register tags (ptags).
 *
 * @param archIdNum The number of architectural register identifiers.
 * @param ptagNum   The total number of physical register tags (ptags).
 */
class RegMap(archIdNum: Int, ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val readIndex = Input(UInt(log2Ceil(archIdNum + 1).W))
    val readData = Output(UInt(log2Ceil(ptagNum + 1).W))
    val writeEnable = Input(Bool())
    val writeIndex = Input(UInt(log2Ceil(archIdNum + 1).W))
    val writeData = Input(UInt(log2Ceil(ptagNum + 1).W))
  })

  // Represents an invalid ptag value used for initializing the register map.
  val REG_MAP_INVALID_VAL = ptagNum.U(log2Ceil(ptagNum + 1).W)

  // Initialize the register map with the invalid ptag value.
  val regMap = RegInit(VecInit(Seq.fill(archIdNum)(REG_MAP_INVALID_VAL)))

  io.readData := regMap(io.readIndex)

  when(io.writeEnable) {
    regMap(io.writeIndex) := io.writeData
  }
}
