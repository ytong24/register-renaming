/** *************************************************************************************
 * File         : RegRenamingTable.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Scala implementation of Register Renaming Table
 * ************************************************************************************* */

package reg_renaming.model.reg_renaming_table

import reg_renaming.model.{Op, OpConfig}

case class RegRenamingTableConfig(ptagNum: Int)

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
    val entry = _regFile.getRegFileEntry(ptag)
    entry.setRegState(RegFileEntryState.COMMIT)

    val prev_ptag = entry.getPrevSameArchId
    val prev_entry = _regFile.getRegFileEntry(prev_ptag)
    prev_entry.setRegState(RegFileEntryState.DEAD)
    releaseEntry(prev_entry)
  }

  private def lookupEntry(index: Int): RegFileEntry = {
    val ptag = _regMap.getPtag(index)
    _regFile.getRegFileEntry(ptag)
  }

  private def readEntry(op: Op, entry: RegFileEntry, index: Int): Unit = {
    val value = entry.getRegPtag
    op.setPtagSrcId(index, value)
  }

  private def writeEntry(op: Op, entry: RegFileEntry, index: Int): Unit = {
    // TODO
    ???
  }

  private def allocEntry(): RegFileEntry = {
    // TODO
    ???
  }

  private def releaseEntry(entry: RegFileEntry): Unit = {
    // TODO
    ???
  }
}
