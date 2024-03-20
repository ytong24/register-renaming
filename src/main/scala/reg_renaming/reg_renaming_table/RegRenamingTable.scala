package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._
import reg_renaming.{Op, OpConfig}

case class RegRenamingTableConfig(ptagNum: Int)

object OpProcessState extends ChiselEnum {
  val idle, readSrc, writeDst, done = Value
}

object OpCommitState extends ChiselEnum {
  val idle, commit = Value
}

class RegRenamingTableIO(opConfig: OpConfig) extends Bundle {
  val op = new Op(opConfig)
  val mode = Input(UInt(2.W))
  val available = Output(Bool())
  val done = Output(Bool())
}

class RegRenamingTable(tableConfig: RegRenamingTableConfig, opConfig: OpConfig) extends Module {
  val io = IO(new RegRenamingTableIO(opConfig))

  val regMap = Module(new RegMap(opConfig.archIdNum))
  val regFile = Module(new RegFile(tableConfig.ptagNum))
  val freeList = Module(new FreeList(tableConfig.ptagNum))

  val ptagSrcIds = RegInit(VecInit(Seq.fill(opConfig.numSrcMax)(0.U(log2Ceil(tableConfig.ptagNum + 1).W))))
  val ptagDstIds = RegInit(VecInit(Seq.fill(opConfig.numDstMax)(0.U(log2Ceil(tableConfig.ptagNum + 1).W))))

  val ioDone = RegInit(false.B)

  io.done := ioDone
  io.available := false.B
  io.op.ptagSrcIds := ptagSrcIds
  io.op.ptagDstIds := ptagDstIds
  //  io.op.out.ptagSrcIds := ptagSrcIds
  //  io.op.out.ptagDstIds := ptagDstIds
  //  io.op.bits.out.ptagSrcIds := ptagSrcIds
  //  io.op.bits.out.ptagDstIds := ptagDstIds
  //  io.op.valid := false.B

  /** initialize regMap */
  val regMapReadIndex = WireDefault(UInt(log2Ceil(opConfig.archIdNum + 1).W), 0.U)
  val regMapWriteEnable = WireDefault(Bool(), false.B)
  val regMapWriteIndex = WireDefault(UInt(log2Ceil(opConfig.archIdNum + 1).W), 0.U)
  val regMapWriteData = WireDefault(UInt(log2Ceil(opConfig.archIdNum + 1).W), 0.U)

  regMap.io.readIndex := regMapReadIndex
  regMap.io.writeEnable := regMapWriteEnable
  regMap.io.writeIndex := regMapWriteIndex
  regMap.io.writeData := regMapWriteData

  /** initialize freeList */
  val freeListPush = Wire(Bool())
  val freeListPop = Wire(Bool())
  val freeListPtagToPush = Wire(UInt(log2Ceil(tableConfig.ptagNum + 1).W))
  freeListPush := false.B
  freeListPop := false.B
  freeListPtagToPush := 0.U

  freeList.io.push := freeListPush
  freeList.io.pop := freeListPop
  freeList.io.ptagToPush := freeListPtagToPush

  /** initialize regFile */
  val regFileIndex = Wire(UInt(log2Ceil(tableConfig.ptagNum + 1).W))
  regFileIndex := 0.U

  val regFileWriteEnable = Wire(Bool())
  regFileWriteEnable := false.B

  val regFileWriteValue = Wire(new RegFileEntry(log2Ceil(tableConfig.ptagNum)))
  regFileWriteValue.regPtag := 0.U
  regFileWriteValue.regArchId := 0.U
  regFileWriteValue.prevSameArchId := 0.U
  regFileWriteValue.regState := RegFileEntryState.FREE

  regFile.io.index := regFileIndex
  regFile.io.writeEnable := regFileWriteEnable
  regFile.io.writeValue := regFileWriteValue

  /** helper variables for helper functions */
  private val opProcessState = RegInit(OpProcessState.idle)
  private val opCommitState = RegInit(OpCommitState.idle)
  private val srcIndex = RegInit(0.U(log2Ceil(opConfig.numSrcMax + 1).W))
  private val dstIndex = RegInit(0.U(log2Ceil(opConfig.numDstMax + 1).W))


  switch(io.mode) {
    is(0.U) {
      //      io.done := processOp(io.op.bits)
      ioDone := processOp(io.op)
    }
    is(1.U) {
      //      io.done := commitOp(io.op.bits)
      ioDone := commitOp(io.op)
    }
    is(2.U) {
      // available when the number of unused slots in the freeList exceeds the op's requirement
      //      io.available := freeList.io.size >= io.op.bits.numDst
      io.available := freeList.io.size >= io.op.numDst
    }
  }


  /** helper functions for op process */
  private def processOp(op: Op): Bool = {
    //    val done = Wire(Bool())
    //    done := false.B
    val done = RegInit(false.B)

    switch(opProcessState) {
      is(OpProcessState.idle) {
        printf(p"State transition: idle -> readSrc\n")
        done := false.B
        // start to readSrc, initialize srcIndex and dstIndex
        opProcessState := OpProcessState.readSrc
        srcIndex := 0.U
        dstIndex := 0.U
      }

      is(OpProcessState.readSrc) {
        srcIndex := readSrc(op, srcIndex)
        printf(p"srcIndex: ${srcIndex}\n")
        when(srcIndex === op.numSrc) {
          printf(p"State transition: readSrc -> writeDst\n")
          opProcessState := OpProcessState.writeDst
        }
      }

      is(OpProcessState.writeDst) {
        dstIndex := writeDst(op, dstIndex)
        when(dstIndex === op.numDst) {
          printf(p"State transition: writeDst -> done\n")
          opProcessState := OpProcessState.done
        }
      }

      is(OpProcessState.done) {
        opProcessState := OpProcessState.idle
        done := true.B
      }
    }
    done
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

        srcIndex +& 1.U
      },
      srcIndex
    )
  }

  private def lookupEntry(archSrcId: UInt): RegFileEntry = {
    // Get the ptag from regMap
    regMapReadIndex := archSrcId
    regMapWriteEnable := false.B
    val ptag = regMap.io.readData

    // Get the entry from regFile
    regFileIndex := ptag
    regFileWriteEnable := false.B
    regFile.io.readValue
  }

  private def readEntry(op: Op, entry: RegFileEntry, srcIndex: UInt): Unit = {
    val ptagSrcId = Wire(UInt(log2Ceil(tableConfig.ptagNum + 1).W))
    ptagSrcId := entry.regPtag
    ptagSrcIds(srcIndex) := ptagSrcId
  }

  private def writeDst(op: Op, dstIndex: UInt): UInt = {
    Mux(dstIndex < op.numDst,
      {
        // Allocate a new entry
        val entry = allocEntry()

        // Write the entry and set the ptagDstId in op
        writeEntry(op, entry, dstIndex)

        dstIndex +& 1.U
      },
      dstIndex
    )
  }

  private def allocEntry(): RegFileEntry = {
    // Get a free ptag from the free list
    freeListPop := true.B
    val ptag = freeList.io.ptagPopped

    // Get the entry from regFile
    regFileIndex := ptag
    regFileWriteEnable := false.B
    val entry = Wire(new RegFileEntry(log2Ceil(opConfig.numDstMax)))
    val readEntry = regFile.io.readValue

    // Set the entry state as ALLOC
    entry.regState := RegFileEntryState.ALLOC
    entry.regPtag := readEntry.regPtag
    entry.regArchId := readEntry.regArchId
    entry.prevSameArchId := readEntry.prevSameArchId

    entry
  }

  private def writeEntry(op: Op, entry: RegFileEntry, dstIndex: UInt): Unit = {
    // Write the ptag to op.ptagDstIds
    val ptag = entry.regPtag
    ptagDstIds(dstIndex) := ptag

    // Track the previous ptag with the same archId
    val archId = op.archDstIds(dstIndex)
    entry.regArchId := archId
    regMapReadIndex := archId
    regMapWriteEnable := false.B
    val prevPtag = regMap.io.readData
    entry.prevSameArchId := prevPtag

    // Update the regMap
    regMapWriteIndex := archId
    regMapWriteData := ptag
    regMapWriteEnable := true.B

    // Write the updated entry back to regFile
    regFileIndex := ptag
    regFileWriteValue := entry
    regFileWriteEnable := true.B
  }

  /** helper functions for op commit * */
  def commitOp(op: Op): Bool = {
    val commitDone = Wire(Bool())
    commitDone := false.B

    switch(opCommitState) {
      is(OpCommitState.idle) {
        opCommitState := OpCommitState.commit
        dstIndex := 0.U
      }

      is(OpCommitState.commit) {
        when(dstIndex < op.numDst) {
          val ptag = op.ptagDstIds(dstIndex)
          removePrev(ptag)
          dstIndex := dstIndex +& 1.U
        }.otherwise {
          opCommitState := OpCommitState.idle
          commitDone := true.B
        }
      }
    }

    commitDone
  }

  private def removePrev(ptag: UInt): Unit = {
    // Set current entry state as COMMIT
    regFileIndex := ptag
    regFileWriteEnable := true.B
    val entry = Wire(new RegFileEntry(log2Ceil(opConfig.numDstMax)))
    entry := regFile.io.readValue
    entry.regState := RegFileEntryState.COMMIT
    regFileWriteValue := entry

    // Release the previous entry with the same architectural register ID
    val prevPtag = entry.prevSameArchId
    regFileIndex := prevPtag
    val prevEntry = Wire(new RegFileEntry(log2Ceil(opConfig.numDstMax)))
    prevEntry := regFile.io.readValue
    prevEntry.regState := RegFileEntryState.DEAD
    regFileWriteValue := prevEntry
    releaseEntry(prevEntry)
  }

  private def releaseEntry(entry: RegFileEntry): Unit = {
    // Reset the register entry
    entry.regState := RegFileEntryState.FREE
    entry.regArchId := 0.U
    entry.prevSameArchId := 0.U
    regFileIndex := entry.regPtag
    regFileWriteValue := entry
    regFileWriteEnable := true.B

    // Return the free entry back to the free list
    freeListPush := true.B
    freeListPtagToPush := entry.regPtag
  }
}
