package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

object RegFileEntryState extends ChiselEnum {
  val FREE, ALLOC, COMMIT, DEAD = Value
}

class RegFileEntry(w: Int) extends Bundle {
  val regPtag = UInt((w + 1).W)
  val regArchId = UInt((w + 1).W)
  val prevSameArchId = UInt((w + 1).W)
  val regState = RegFileEntryState()
}

class RegFile(ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val index = Input(UInt(log2Ceil(ptagNum + 1).W))
    val writeEnable = Input(Bool())
    val writeValue = Input(new RegFileEntry(log2Ceil(ptagNum)))
    val readValue = Output(new RegFileEntry(log2Ceil(ptagNum)))
  })

  val regFileEntries = RegInit(VecInit((0 until ptagNum).map { i =>
    val entry = Wire(new RegFileEntry(log2Ceil(ptagNum)))
    entry.regPtag := i.U
    entry.regArchId := 0.U
    entry.prevSameArchId := 0.U
    entry.regState := RegFileEntryState.FREE
    entry
  }))
  val readValueReg = Reg(new RegFileEntry(log2Ceil(ptagNum)))

  readValueReg := regFileEntries(io.index)
  io.readValue := readValueReg

  when(io.writeEnable) {
    regFileEntries(io.index) := io.writeValue
  }
}
