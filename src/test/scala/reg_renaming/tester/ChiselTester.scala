package reg_renaming.tester

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import reg_renaming.OpConfig
import reg_renaming.reg_renaming_table._

class ChiselTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FreeList"
  it should "be initialized with the correct size" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.size.expect(5.U, "Initial FreeList size should be equal to ptagNum")
    }
  }

  it should "decrease in size by one after a pop" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.size.expect(4.U, "FreeList size should decrease by 1 after pop")
    }
  }

  it should "increase in size by one after a push" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      // First pop to decrease size
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      // Then push to increase size
      dut.io.push.poke(true.B)
      dut.io.ptagToPush.poke(0.U) // Assuming 0 is a valid ptag to push
      dut.clock.step()
      dut.io.size.expect(5.U, "FreeList size should increase by 1 after push")
    }
  }

  it should "pop values within valid range" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      val poppedValue = dut.io.ptagPopped.peek().litValue
      assert(poppedValue >= 0 && poppedValue < 5, "Popped value should be within valid range")
    }
  }


  it should "restore its size after popping and then pushing" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      // First pop to decrease size
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      // Then push to increase size
      dut.io.push.poke(true.B)
      dut.io.ptagToPush.poke(dut.io.ptagPopped.peek().litValue.U) // Re-push the popped value
      dut.clock.step()
      dut.io.size.expect(5.U, "FreeList size should be restored after pushing the popped element back")
    }
  }

  it should "pop the correct values in sequence" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      // pop 3 values
      for (i <- 0 until 3) {
        dut.io.push.poke(false.B)
        dut.io.pop.poke(true.B)
        dut.clock.step()

        val expectedValue = i.U
        dut.io.ptagPopped.expect(expectedValue, s"Expected $expectedValue")
      }

      dut.io.size.expect((5 - 3).U, "FreeList size should decrease by 3 after popping 3 elements")
    }
  }


  behavior of "RegFile"
  it should "each RegFileEntry has default value" in {
    test(new RegFile(archIdNum = 4, ptagNum = 8)) { dut =>
      for (i <- 0 until 8) {
        dut.io.index.poke(i.U)
        dut.io.writeEnable.poke(false.B)
        dut.clock.step()

        dut.io.readValue.regPtag.expect(i.U)
        dut.io.readValue.regArchId.expect(4.U)
        dut.io.readValue.prevSameArchId.expect(4.U)
        dut.io.readValue.regState.expect(RegFileEntryState.FREE)
      }
    }
  }

  it should "return a valid RegFileEntry for a valid index" in {
    test(new RegFile(archIdNum = 5, ptagNum = 5)) { dut =>
      dut.io.index.poke(0.U)
      dut.io.writeEnable.poke(true.B)
      dut.io.writeValue.regPtag.poke(1.U)
      dut.io.writeValue.regArchId.poke(1.U)
      dut.io.writeValue.prevSameArchId.poke(0.U)
      dut.io.writeValue.regState.poke(RegFileEntryState.ALLOC)
      dut.clock.step()

      dut.io.index.poke(0.U)
      dut.io.writeEnable.poke(false.B)
      dut.clock.step()

      dut.io.readValue.regPtag.expect(1.U, "Retrieved RegFileEntry should have a regPtag of 1")
      dut.io.readValue.regArchId.expect(1.U, "Retrieved RegFileEntry should have a regArchId of 10")
      dut.io.readValue.prevSameArchId.expect(0.U, "Retrieved RegFileEntry should have a prevSameArchId of 20")
      dut.io.readValue.regState.expect(RegFileEntryState.ALLOC, "Retrieved RegFileEntry should be in ALLOC state")
    }
  }

  it should "correctly set a RegFileEntry at a specific index" in {
    test(new RegFile(archIdNum = 5, ptagNum = 5)) { dut =>
      dut.io.index.poke(1.U)
      dut.io.writeEnable.poke(true.B)
      dut.io.writeValue.regPtag.poke(2.U)
      dut.io.writeValue.regArchId.poke(2.U)
      dut.io.writeValue.prevSameArchId.poke(1.U)
      dut.io.writeValue.regState.poke(RegFileEntryState.COMMIT)
      dut.clock.step()

      dut.io.index.poke(1.U)
      dut.io.writeEnable.poke(false.B)
      dut.clock.step()

      dut.io.readValue.regPtag.expect(2.U, "Set RegFileEntry should have a regPtag of 2")
      dut.io.readValue.regArchId.expect(2.U, "Set RegFileEntry should have a regArchId of 30")
      dut.io.readValue.prevSameArchId.expect(1.U, "Set RegFileEntry should have a prevSameArchId of 40")
      dut.io.readValue.regState.expect(RegFileEntryState.COMMIT, "Set RegFileEntry should be in COMMIT state")
    }
  }


  behavior of "RegMap"
  it should "initialize with default values" in {
    test(new RegMap(archIdNum = 4, ptagNum = 8)) { dut =>
      for (i <- 0 until 4) {
        dut.io.readIndex.poke(i.U)
        dut.clock.step()
        dut.io.readData.expect(8.U, s"RegMap entry $i should be initialized to invalid value")
      }
    }
  }

  it should "set and get a ptag value correctly" in {
    test(new RegMap(archIdNum = 3, ptagNum = 3)) { dut =>
      val testIndex = 2.U // Arbitrary index to test
      val testValue = 1.U // Arbitrary value to set

      dut.io.writeEnable.poke(true.B)
      dut.io.writeIndex.poke(testIndex)
      dut.io.writeData.poke(testValue)
      dut.clock.step()
      dut.io.writeEnable.poke(false.B)

      dut.io.readIndex.poke(testIndex)
      dut.clock.step()
      dut.io.readData.expect(testValue, s"RegMap entry $testIndex should be $testValue after setPtag")
    }
  }


  behavior of "RegRenamingTable"
  val tableConfig = RegRenamingTableConfig(ptagNum = 4)
  val opConfig = OpConfig(archIdNum = 3, numSrcMax = 2, numDstMax = 2)

  it should "process op with 0 src and 2 dst" in {
    test(new RegRenamingTable(tableConfig, opConfig)) { dut =>

      dut.io.op.numSrc.poke(0.U)
      dut.io.op.numDst.poke(2.U)

      dut.io.op.archDstIds(0).poke(0.U)
      dut.io.op.archDstIds(1).poke(1.U)

      dut.io.mode.poke(0.U)

      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.op.ptagDstIds(0).expect(0.U) // p0
      dut.io.op.ptagDstIds(1).expect(1.U) // p1
    }
  }

  it should "process op with 2 src and 1 dst" in {
    test(new RegRenamingTable(tableConfig, opConfig)) { dut =>

      // initialize the RegRenamingTable by processing an op with 0 src and 2 dst
      dut.io.op.numSrc.poke(0.U)
      dut.io.op.numDst.poke(2.U)

      dut.io.op.archDstIds(0).poke(0.U) // r0
      dut.io.op.archDstIds(1).poke(1.U) // r1
      dut.io.mode.poke(0.U)
      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.op.ptagDstIds(0).expect(0.U) // p0
      dut.io.op.ptagDstIds(1).expect(1.U) // p1

      // Wait for a clock cycle before starting the next operation
      dut.clock.step()

      // process an op with 2 src and 1 dst
      dut.io.op.numSrc.poke(2.U)
      dut.io.op.numDst.poke(1.U)

      dut.io.op.archSrcIds(0).poke(0.U) // r0
      dut.io.op.archSrcIds(1).poke(1.U) // r1
      dut.io.op.archDstIds(0).poke(2.U) // r2

      dut.io.mode.poke(0.U)

      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.op.ptagSrcIds(0).expect(0.U) // p0
      dut.io.op.ptagSrcIds(1).expect(1.U) // p1
      dut.io.op.ptagDstIds(0).expect(2.U) // p2
    }
  }

  it should "reuse the ptags after commit" in {
    test(new RegRenamingTable(tableConfig, opConfig)) { dut =>

      // initialize the RegRenamingTable by processing an op with 0 src and 2 dst
      dut.io.op.numSrc.poke(0.U)
      dut.io.op.numDst.poke(2.U)

      dut.io.op.archDstIds(0).poke(0.U) // r0
      dut.io.op.archDstIds(1).poke(1.U) // r1
      dut.io.mode.poke(0.U)
      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.op.ptagDstIds(0).expect(0.U) // p0
      dut.io.op.ptagDstIds(1).expect(1.U) // p1

      // Wait for a clock cycle before starting the next operation
      dut.clock.step()

      // commit the above op
      dut.io.mode.poke(1)
      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }

      // process an op with 0 src and 1 dst, should reuse p0
      dut.io.op.numSrc.poke(0.U)
      dut.io.op.numDst.poke(1.U)

      dut.io.op.archDstIds(0).poke(0.U) // r0
      dut.io.mode.poke(0.U)
      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.op.ptagDstIds(0).expect(0.U) // reuse p0
    }
  }
}
