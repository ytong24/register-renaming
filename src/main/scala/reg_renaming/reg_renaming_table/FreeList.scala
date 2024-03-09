package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

class FreeList(val ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val push = Input(Bool())
    val pop = Input(Bool())
    val ptagToPush = Input(UInt(log2Ceil(ptagNum + 1).W))
    val ptagPopped = Output(UInt(log2Ceil(ptagNum + 1).W))
    val size = Output(UInt(log2Ceil(ptagNum + 1).W))
  })

  // FIXME: elements should be 0..ptagNum-1
  val stack = RegInit(VecInit(Seq.fill(ptagNum)(0.U(log2Ceil(ptagNum + 1).W))))
  val pointer = RegInit(0.U(log2Ceil(ptagNum + 1).W))

  // Push operation
  when(io.push && !io.pop && pointer =/= ptagNum.U) {
    stack(pointer) := io.ptagToPush
    pointer := pointer + 1.U
  }

  // Pop operation
  io.ptagPopped := 0.U // Default value
  when(io.pop && !io.push && pointer =/= 0.U) {
    pointer := pointer - 1.U
    io.ptagPopped := stack(pointer)
  }

  // Update size
  io.size := ptagNum.U -& pointer
}

