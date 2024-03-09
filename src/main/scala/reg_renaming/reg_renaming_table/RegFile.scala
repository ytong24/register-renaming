package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

object RegFileEntryState extends ChiselEnum {
  val FREE, ALLOC, PRODUCE, COMMIT, DEAD = Value
}

class RegFileEntry(w: Int) extends Bundle {
  val regPtag = UInt(w.W)
  val regArchId = RegInit(0.U(w.W))
  val prevSameArchId = RegInit(0.U(w.W))
  val regState = RegInit(RegFileEntryState.FREE)
}

class RegFile(ptagNum: Int, w: Int) extends Module {
  val io = IO(new Bundle {
    val index = Input(UInt(log2Ceil(ptagNum).W))
    val writeEnable = Input(Bool())
    val writeValue = Input(new RegFileEntry(w))
    val readValue = Output(new RegFileEntry(w))
  })

  val regFileEntries = Reg(Vec(ptagNum, new RegFileEntry(w)))

  io.readValue := regFileEntries(io.index)

  when(io.writeEnable) {
    regFileEntries(io.index) := io.writeValue
  }
}
