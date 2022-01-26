#!/bin/sh -v

URI=${URI-tmr:///com11}
date

./Demo $URI get
./Samples/Codelets/Read/bin/Debug/Read $URI 
./Samples/Codelets/BlockPermaLock/bin/Debug/BlockPermaLock $URI 
./Samples/Codelets/BlockWrite/bin/Debug/BlockWrite $URI 
./Samples/Codelets/SavedConfig/bin/Debug/SavedConfig.exe $URI 
./Samples/Codelets/MultiProtocolRead/bin/Debug/MultiProtocolRead $URI 
./Samples/Codelets/ReadAsync/bin/Debug/ReadAsync $URI 




