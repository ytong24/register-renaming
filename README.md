<h1>Register Naming</h1>
<h2>Overview</h2>
   We implemented Register Renaming, which is the allocation stage in the pipeline of the microarchitecture of processors.
   Name dependencies are caused by the reuse of registers without involving any data dependencies. 
   To maximize the instruction-level parallelism,Register Renaming entails changing the names of the register to avoid false dependencies.

<h2>Verification</h2>
<h3>Testing</h3>
Do the following command to run the test cases: 
```dtd
sbt test
```
<h3>Scenario</h3>
FETCH -> <b>RENAMING</b> -> ISSUE -> EXECUTE -> COMMIT

<h2>Design</h2>
   <h3>Structure</h3>
      <ul>
        <li>Op</li>
        <li>RegisterRenamingTable
            <ul>
                <li>RegMap</li>
                <li>RegFile</li>
                <li>FreeList</li>
            </ul>
        </li>
      </ul>

   <h3>Interface</h3>
      <ul>
        <li>Available
            <ul>
                <li>Before FETCH stage, the processor first checks if there are enough free registers.</li>
            </ul>
        </li>
        <li>Process
            <ul>
                <li>After an operand is fetched, it reads data dependency and applies allocation in the RENAMING stage.</li>
            </ul>
        </li>
        <li>Commit
            <ul>
                <li>Once an operand is retired, the previous registers shared same architectural ids can be released.</li>
            </ul>
        </li>
      </ul>