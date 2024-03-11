package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._
import reg_renaming.{Op, OpConfig}

case class RegRenamingTableConfig(ptagNum: Int)

object OpProcessState extends ChiselEnum {
  val idle, readSrc, writeDst, done = Value
}

class RegRenamingTable(tableConfig: RegRenamingTableConfig, opConfig: OpConfig) extends Module {
  val io = IO {
    new Bundle {
      val op = Flipped(Decoupled(new Op(opConfig)))
      val mode = Input(UInt(2.W)) // 0: process, 1: commit, 2: available.
      val available = Output(Bool()) // when mode==2, set this output
      val done = Output(Bool())
    }
  }

  val regMap = Module(new RegMap(opConfig.archIdNum))
  val regFile = Module(new RegFile(tableConfig.ptagNum))
  val freeList = Module(new FreeList(tableConfig.ptagNum))

  io.done := false.B
  io.available := false.B


  switch(io.mode) {
    is(0.U) {
      io.done := processOp(io.op.bits)
    }
    is(1.U) {
      io.done := commitOp(io.op.bits)
    }
    is(2.U) {
      // available when the number of unused slots in the freeList exceeds the op's requirement
      io.available := freeList.io.size >= io.op.bits.numDst
    }
  }

  /** helper variables for helper functions */
  private val opProcessState = RegInit(OpProcessState.idle)
  private val srcIndex = RegInit(0.U(log2Ceil(opConfig.numSrcMax).W))
  private val dstIndex = RegInit(0.U(log2Ceil(opConfig.numDstMax).W))

  /** helper functions for op process */
  private def processOp(op: Op): Bool = {
    switch(opProcessState) {
      is(OpProcessState.idle) {
        // start to readSrc, initialize srcIndex and dstIndex
        opProcessState := OpProcessState.readSrc
        srcIndex := 0.U
        dstIndex := 0.U
      }

      is(OpProcessState.readSrc) {
        srcIndex := readSrc(op, srcIndex)
        when(srcIndex === op.numSrc) {
          opProcessState := OpProcessState.writeDst
        }
      }

      is(OpProcessState.writeDst) {
        dstIndex := writeDst(op, dstIndex)
        when(dstIndex === op.numDst) {
          opProcessState := OpProcessState.done
        }
      }

      is(OpProcessState.done) {
        opProcessState := OpProcessState.idle
        return true.B
      }
    }
    false.B
  }

  private def readSrc(op: Op, srcIndex: UInt): UInt = {
    Mux(srcIndex < op.numSrc,
      {
        // Get the archSrcId
        val archSrcId = op.archSrcIds(srcIndex)

        // Get the ptag from regMap
        val entry = lookupEntry(archSrcId)

        // Get the entry from regFile and set the ptagSrcId in op
        readEntry(op, entry, srcIndex)

        srcIndex + 1.U
      },
      srcIndex
    )
  }

  private def lookupEntry(archSrcId: UInt): RegFileEntry = {
    // Get the ptag from regMap
    regMap.io.readIndex := archSrcId
    regMap.io.writeEnable := false.B
    val ptag = regMap.io.readData

    // Get the entry from regFile
    regFile.io.index := ptag
    regFile.io.writeEnable := false.B
    regFile.io.readValue
  }

  private def readEntry(op: Op, entry: RegFileEntry, srcIndex: UInt): Unit = {
    op.ptagSrcIds(srcIndex) := entry.regPtag
  }

  private def writeDst(op: Op, dstIndex: UInt): UInt = {
    Mux(dstIndex < op.numDst,
      {
        // Allocate a new entry
        val entry = allocEntry()

        // Write the entry and set the ptagDstId in op
        writeEntry(op, entry, dstIndex)

        dstIndex + 1.U
      },
      dstIndex
    )
  }

  private def allocEntry(): RegFileEntry = {
    // Get a free ptag from the free list
    freeList.io.pop := true.B
    val ptag = freeList.io.ptagPopped

    // Get the entry from regFile
    regFile.io.index := ptag
    regFile.io.writeEnable := false.B
    //  not sure if we need to do this: val entry = Wire(new RegFileEntry(log2Ceil(config.numDstMax)))
    val entry = regFile.io.readValue

    // Set the entry state as ALLOC
    entry.regState := RegFileEntryState.ALLOC

    entry
  }

  private def writeEntry(op: Op, entry: RegFileEntry, dstIndex: UInt): Unit = {
    // Write the ptag to op.ptagDstIds
    val ptag = entry.regPtag
    op.ptagDstIds(dstIndex) := ptag

    // Track the previous ptag with the same archId
    val archId = op.archDstIds(dstIndex)
    entry.regArchId := archId
    regMap.io.readIndex := archId
    regMap.io.writeEnable := false.B
    val prevPtag = regMap.io.readData
    entry.prevSameArchId := prevPtag

    // Update the regMap
    regMap.io.writeIndex := archId
    regMap.io.writeData := ptag
    regMap.io.writeEnable := true.B

    // Write the updated entry back to regFile
    regFile.io.index := ptag
    regFile.io.writeValue := entry
    regFile.io.writeEnable := true.B
  }

  /** helper functions for op commit * */
  def commitOp(op: Op): Bool = {
    true.B
  }

}
