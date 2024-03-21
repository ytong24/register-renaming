package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

class RegMap(archIdNum: Int, ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val readIndex = Input(UInt(log2Ceil(archIdNum + 1).W))
    val readData = Output(UInt(log2Ceil(ptagNum + 1).W))
    val writeEnable = Input(Bool())
    val writeIndex = Input(UInt(log2Ceil(archIdNum + 1).W))
    val writeData = Input(UInt(log2Ceil(ptagNum + 1).W))
  })

  val REG_MAP_INVALID_VAL = ptagNum.U(log2Ceil(ptagNum + 1).W)
  val regMap = RegInit(VecInit(Seq.fill(archIdNum)(REG_MAP_INVALID_VAL)))

  io.readData := regMap(io.readIndex)

  when(io.writeEnable) {
    regMap(io.writeIndex) := io.writeData
  }
}
