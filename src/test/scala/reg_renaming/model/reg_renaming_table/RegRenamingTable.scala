/** *************************************************************************************
 * File         : RegRenamingTable.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Scala implementation of Register Renaming Table
 * ************************************************************************************* */

package reg_renaming.model.reg_renaming_table

import reg_renaming.OpConfig
import reg_renaming.model.Op
import reg_renaming.reg_renaming_table.RegRenamingTableConfig



class RegRenamingTable(tableConfig: RegRenamingTableConfig, opConfig: OpConfig) {
  private var _regMap = new RegMap(opConfig)
  private var _regFile = new RegFile(tableConfig)
  private var _freeList = new FreeList(tableConfig)

  def available(): Boolean = {
    _freeList.size() >= opConfig.numDstMax
  }

  def process(op: Op): Unit = {
    readSrc(op)
    writeDst(op)
  }

  def commit(op: Op): Unit = {
    for (index <- 0 until op.getNumDst) {
      removePrev(op.getPtagDstId(index))
    }
  }

  def getRegMap: RegMap = {
    _regMap
  }

  def getRegFile: RegFile = {
    _regFile
  }

  def getFreeList: FreeList = {
    _freeList
  }

  private def readSrc(op: Op): Unit = {
    for (index <- 0 until op.getNumSrc) {
      val entry = lookupEntry(op.getArchSrcId(index))
      readEntry(op, entry, index)
    }
  }

  private def writeDst(op: Op): Unit = {
    for (index <- 0 until op.getNumDst) {
      val entry = allocEntry()
      writeEntry(op, entry, index)
    }
  }

  private def removePrev(ptag: Int): Unit = {
    // Set Current Entry State as COMMIT
    val entry = _regFile.getRegFileEntry(ptag)
    require(entry.getRegState == RegFileEntryState.ALLOC)
    entry.setRegState(RegFileEntryState.COMMIT)
    // Release the Previous Entry With Same Architectural Register ID
    val prev_ptag = entry.getPrevSameArchId
    val prev_entry = _regFile.getRegFileEntry(prev_ptag)
    prev_entry.setRegState(RegFileEntryState.DEAD)
    releaseEntry(prev_entry)
  }

  private def lookupEntry(index: Int): RegFileEntry = {
    // Get the Latest Physical Register ID (Ptag) through the Register Map
    val ptag = _regMap.getPtag(index)
    // Get the Physical Entry through the Register File
    _regFile.getRegFileEntry(ptag)
  }

  private def readEntry(op: Op, entry: RegFileEntry, index: Int): Unit = {
    // Write the Physical Register ID (Ptag) Op Src
    val value = entry.getRegPtag
    require(op.getPtagSrcId(index) == -1)
    op.setPtagSrcId(index, value)
  }

  private def writeEntry(op: Op, entry: RegFileEntry, index: Int): Unit = {
    // Write the Physical Register ID (Ptag) to Op Dst
    val ptag = entry.getRegPtag
    require(op.getPtagDstId(index) == -1)
    op.setPtagDstId(index, ptag)
    // Track the Previous Physical Register ID (Ptag) with Same Architectural ID
    val archId = op.getArchDstId(index)
    entry.setRegArchId(archId)
    val prevSameArchId = _regMap.getPtag(archId)
    entry.setPrevSameArchId(prevSameArchId)
    // Update the Map to the Latest Physical Register ID (Ptag)
    _regMap.setPtag(archId, ptag)
  }

  private def allocEntry(): RegFileEntry = {
    // Get a Free Entry From the Free List
    val ptag = _freeList.pop()
    val entry = _regFile.getRegFileEntry(ptag)
    // Set the Entry State as ALLOC
    require(entry.getRegState == RegFileEntryState.FREE)
    entry.setRegState(RegFileEntryState.ALLOC)
    entry
  }

  private def releaseEntry(entry: RegFileEntry): Unit = {
    // Reset the Register Entry
    entry.setRegState(RegFileEntryState.FREE)
    entry.setRegArchId(-1)
    entry.setPrevSameArchId(-1)
    // Return the Free Entry Back to the Free List
    _freeList.push(entry.getRegPtag)
  }
}
