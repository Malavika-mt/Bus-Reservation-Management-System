@echo off
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java || exit /b 1
java -cp "out;lib/*" MainApplication %*
