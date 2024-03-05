# Register Naming
## Overview
We implemented Register Renaming, which is the allocation stage in the pipeline of the microarchitecture of processors. Name dependencies are caused by the reuse of registers without involving any data dependencies. To maximize the instruction-level parallelism, Register Renaming entails changing the names of the register to avoid false dependencies.


## Implementation
### Structure
 - Op
 - RegisterRenamingTable
   - RegMap
   - RegFile
   - FreeList

### Dependency
FETCH -> RENAMING -> ISSUE -> EXECUTE -> COMMIT

### Interface
 - Available
   - Before FETCH stage, the processor first checks if there are enough free registers.
 - Process
   - After an operand is fetched, it reads data dependency and applies allocation in the RENAMING stage.
 - Commit
   - Once an operand is retired, the previous registers shared same architectural ids can be released. 
