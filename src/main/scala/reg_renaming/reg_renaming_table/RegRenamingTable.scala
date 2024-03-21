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

object ReadSrcState extends ChiselEnum {
  val idle, findPtag, getEntry, readEntry = Value
}

object WriteDstState extends ChiselEnum {
  val idle, newPtag, getEntry, writeEntry = Value
}

class RegRenamingTableIO(opConfig: OpConfig) extends Bundle {
  val op = new Op(opConfig)
  val mode = Input(UInt(2.W))
  val available = Output(Bool())
  val done = Output(Bool())
}

class RegRenamingTable(tableConfig: RegRenamingTableConfig, opConfig: OpConfig) extends Module {
  val io = IO(new RegRenamingTableIO(opConfig))

  val regMap = Module(new RegMap(opConfig.archIdNum, tableConfig.ptagNum))
  val regFile = Module(new RegFile(opConfig.archIdNum, tableConfig.ptagNum))
  val freeList = Module(new FreeList(tableConfig.ptagNum))

  val ptagSrcIds = RegInit(VecInit(Seq.fill(opConfig.numSrcMax)(0.U(log2Ceil(tableConfig.ptagNum + 1).W))))
  val ptagDstIds = RegInit(VecInit(Seq.fill(opConfig.numDstMax)(0.U(log2Ceil(tableConfig.ptagNum + 1).W))))

  val ioDone = RegInit(false.B)

  io.done := ioDone
  io.available := false.B
  io.op.ptagSrcIds := ptagSrcIds
  io.op.ptagDstIds := ptagDstIds

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

  val regFileWriteValue = Wire(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(tableConfig.ptagNum)))
  regFileWriteValue.regPtag := 0.U
  regFileWriteValue.regArchId := 0.U
  regFileWriteValue.prevSameArchId := 0.U
  regFileWriteValue.regState := RegFileEntryState.FREE

  regFile.io.index := regFileIndex
  regFile.io.writeEnable := regFileWriteEnable
  regFile.io.writeValue := regFileWriteValue

  /** helper variables for helper functions */
  private val opProcessState = RegInit(OpProcessState.idle)
  private val readSrcState = RegInit(ReadSrcState.idle)
  private val writeDstState = RegInit(WriteDstState.idle)

  private val opCommitState = RegInit(OpCommitState.idle)
  private val srcIndex = RegInit(0.U(log2Ceil(opConfig.numSrcMax + 1).W))
  private val dstIndex = RegInit(0.U(log2Ceil(opConfig.numDstMax + 1).W))


  switch(io.mode) {
    is(0.U) {
      processOp(io.op)
    }
    is(1.U) {
      commitOp(io.op)
    }
    is(2.U) {
      // available when the number of unused slots in the freeList exceeds the op's requirement
      io.available := freeList.io.size >= io.op.numDst
    }
  }


  /** helper functions for op process */
  private def processOp(op: Op): Unit = {
    switch(opProcessState) {
      is(OpProcessState.idle) {
        printf(p"State transition: idle -> readSrc\n")
        ioDone := false.B
        // start to readSrc, initialize srcIndex and dstIndex
        opProcessState := OpProcessState.readSrc
        srcIndex := 0.U
        dstIndex := 0.U
      }

      is(OpProcessState.readSrc) {
        readSrc(op, srcIndex)
        printf(p"srcIndex: ${srcIndex}\n")
        when(srcIndex === op.numSrc) {
          printf(p"State transition: readSrc -> writeDst\n")
          opProcessState := OpProcessState.writeDst
          srcIndex := 0.U
        }
      }

      is(OpProcessState.writeDst) {
        writeDst(op, dstIndex)
        printf(p"dstIndex: ${dstIndex}\n")
        when(dstIndex === op.numDst) {
          printf(p"State transition: writeDst -> done\n")
          opProcessState := OpProcessState.done
          dstIndex := 0.U
        }
      }

      is(OpProcessState.done) {
        opProcessState := OpProcessState.idle
        ioDone := true.B
      }
    }
  }

  private def readSrc(op: Op, srcIndex: UInt): Unit = {
    val readSrcPtag = Reg(UInt(log2Ceil(opConfig.numDstMax+1).W))
    val entry = Reg(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(opConfig.numDstMax)))

    switch(readSrcState) {
      is(ReadSrcState.idle) {
        when(srcIndex < op.numSrc) {
          readSrcState := ReadSrcState.findPtag
          printf(p"readSrcState transition: idle -> findPtag\n")
        }
      }

      is(ReadSrcState.findPtag) {
        val archSrcId = op.archSrcIds(srcIndex)
        printf(p"archSrcId: ${archSrcId}\n")
        printf(p"op.archSrcIds: ${op.archSrcIds}\n")
        findPtag(archSrcId, readSrcPtag)
        readSrcState := ReadSrcState.getEntry
        printf(p"readSrcState transition: findPtag -> getEntry\n")
      }

      is(ReadSrcState.getEntry) {
        printf(p"ptag: ${readSrcPtag}\n")
        entry := getEntry(readSrcPtag)
        readSrcState := ReadSrcState.readEntry
        printf(p"readSrcState transition: getEntry -> readEntry\n")
      }

      is(ReadSrcState.readEntry) {
        readEntry(srcIndex, readSrcPtag)
        srcIndex := srcIndex +& 1.U
        readSrcState := ReadSrcState.idle
        printf(p"readSrcState transition: readEntry -> idle\n")
      }
    }
  }

  private def findPtag(archSrcId: UInt, ptagReg: UInt): Unit = {
    // Get the ptag from regMap
    regMapReadIndex := archSrcId
    regMapWriteEnable := false.B
    ptagReg := regMap.io.readData
  }

  private def readEntry(srcIndex: UInt, ptag: UInt): Unit = {
    ptagSrcIds(srcIndex) := ptag
  }


  private def writeDst(op: Op, dstIndex: UInt): Unit = {
    val writeDstPtag = Reg(UInt(log2Ceil(opConfig.numDstMax + 1).W))
    val entry = Reg(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(opConfig.numDstMax)))

    switch(writeDstState) {
      is(WriteDstState.idle) {
        when(dstIndex < op.numDst) {
          writeDstState := WriteDstState.newPtag
          freeListPush := false.B
          freeListPop := true.B
          writeDstPtag := 0.U
          printf(p"writeDstState transition: idle -> newPtag\n")
        }
      }

      is(WriteDstState.newPtag) {
        writeDstPtag := freeList.io.ptagPopped
        writeDstState := WriteDstState.getEntry
        printf(p"writeDstState transition: newPtag -> getEntry\n")
      }

      is(WriteDstState.getEntry) {
        printf(p"ptag: ${writeDstPtag}\n")
        entry := getEntry(writeDstPtag)
        writeDstState := WriteDstState.writeEntry
        printf(p"writeDstState transition: getEntry -> writeEntry\n")
      }

      is(WriteDstState.writeEntry) {
        writeEntry(op, entry, dstIndex, writeDstPtag)
        dstIndex := dstIndex +& 1.U
        writeDstState := WriteDstState.idle
        printf(p"writeDstState transition: writeEntry -> idle\n")
      }
    }
  }

  private def getEntry(ptag: UInt): RegFileEntry = {
    // Get the entry from regFile
    regFileIndex := ptag
    regFileWriteEnable := false.B
    val entry = Wire(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(opConfig.numDstMax)))
    val readEntry = regFile.io.readValue

    // Set the entry state as ALLOC
    entry.regPtag := readEntry.regPtag
    entry.regArchId := readEntry.regArchId
    entry.prevSameArchId := readEntry.prevSameArchId
    entry.regState := readEntry.regState
    entry
  }

  private def writeEntry(op: Op, entry: RegFileEntry, dstIndex: UInt, ptag: UInt): Unit = {
    // Write the ptag to op.ptagDstIds
    ptagDstIds(dstIndex) := ptag

    entry.regState := RegFileEntryState.ALLOC
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
  def commitOp(op: Op): Unit = {
    ioDone := false.B

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
          ioDone := true.B
        }
      }
    }
  }

  private def removePrev(ptag: UInt): Unit = {
    // Set current entry state as COMMIT
    regFileIndex := ptag
    regFileWriteEnable := true.B
    val entry = Wire(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(opConfig.numDstMax)))
    entry := regFile.io.readValue
    entry.regState := RegFileEntryState.COMMIT
    regFileWriteValue := entry

    // Release the previous entry with the same architectural register ID
    val prevPtag = entry.prevSameArchId
    regFileIndex := prevPtag
    val prevEntry = Wire(new RegFileEntry(log2Ceil(opConfig.archIdNum), log2Ceil(opConfig.numDstMax)))
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
