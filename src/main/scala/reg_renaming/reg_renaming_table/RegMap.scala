package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

class RegMap(archIdNum: Int) extends Module {
  val io = IO(new Bundle {
    val readIndex = Input(UInt(log2Ceil(archIdNum).W))
    val readData = Output(UInt(log2Ceil(archIdNum).W))
    val writeEnable = Input(Bool())
    val writeIndex = Input(UInt(log2Ceil(archIdNum).W))
    val writeData = Input(UInt(log2Ceil(archIdNum).W))
  })

  val regMap = RegInit(VecInit(Seq.fill(archIdNum)(0.U(log2Ceil(archIdNum).W))))

  io.readData := regMap(io.readIndex)

  when(io.writeEnable) {
    regMap(io.writeIndex) := io.writeData
  }
}
