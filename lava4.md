Lava4

Overview
=============
This is a simple JVM, written in Java.  The basic unit of information here is a Cell, which can hold an int,
an object reference or a String.  Internally, this uses SQLite as a database.

Installation
=============
This has dependencies, which I put in lava4/lib.
bcel-6.5.0.jar.  Download from https://commons.apache.org/proper/commons-bcel/download_bcel.cgi.
jna.jar.  Download from https://github.com/java-native-access/jna/tree/master/dist
win32-x86-64.jar
win32-x86-64/sqlite3.dll.  Download from https://www.sqlite.org/download.html.  Get the 64 bit DLL for X86 version.

Compilation
============
I make a compile.bat file that looks like this.  The * will import all the jar files.

rem compile.bat
c:\java\jdk1.8.0_261\bin\javac -classpath c:\java\projects5\;c:\java\projects5\lava4\lib\* %1

Running
============
The run.bat file looks like this. It needs both the \lib\ and \lib\*.
c:\java\jdk1.8.0_261\bin\java -classpath c:\java\projects5\;c:\java\projects5\lava4\lib\;c:\java\projects5\lava4\lib\* -Ddir=c:\java\projects5\ %1 %2

The -Ddir holds the base to user classes.

Testing
===========
There is a test Fibonnaci class in lava4.test. This needs further testing.

