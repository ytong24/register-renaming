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
    // TODO:
    ???
  }

  private def readSrc(op: Op): Unit = {
    // TODO:
    ???
  }

  private def writeDst(op: Op): Unit = {
    // TODO:
    ???
  }

}
