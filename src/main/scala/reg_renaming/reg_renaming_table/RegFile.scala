package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object RegFileEntryState extends ChiselEnum {
  val FREE, ALLOC, COMMIT, DEAD = Value
}

class RegFileEntry(archBit: Int, ptagBit: Int) extends Bundle {
  val regPtag = UInt((ptagBit + 1).W)
  val regArchId = UInt((archBit + 1).W)
  val prevSameArchId = UInt((archBit + 1).W)
  val regState = RegFileEntryState()
}

class RegFile(archIdNum: Int, ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val index = Input(UInt(log2Ceil(ptagNum + 1).W))
    val writeEnable = Input(Bool())
    val writeValue = Input(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))
    val readValue = Output(new RegFileEntry(log2Ceil(archIdNum), log2Ceil(ptagNum)))
  })

  val REG_FILE_INVALID_VAL = archIdNum.U(log2Ceil(archIdNum + 1).W)
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
