package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

object RegFileEntryState extends ChiselEnum {
  val FREE, ALLOC, PRODUCE, COMMIT, DEAD = Value
}

class RegFileEntry(w: Int) extends Bundle {
  val regPtag = UInt(w.W)
  val regArchId = UInt(w.W)
  val prevSameArchId = UInt(w.W)
  val regState = RegFileEntryState()
}

class RegFile(ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val index = Input(UInt(log2Ceil(ptagNum).W))
    val writeEnable = Input(Bool())
    val writeValue = Input(new RegFileEntry(log2Ceil(ptagNum)))
    val readValue = Output(new RegFileEntry(log2Ceil(ptagNum)))
  })

  val regFileEntries = Reg(Vec(ptagNum, new RegFileEntry(log2Ceil(ptagNum))))

  io.readValue := regFileEntries(io.index)

  when(io.writeEnable) {
    regFileEntries(io.index) := io.writeValue
  }
}
