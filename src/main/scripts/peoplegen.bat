@echo off
if "%OS%" == "Windows_NT" @setlocal
if "%OS%" == "WINNT" @setlocal
rem ---------------------------------------------------------------------------
rem Front end Windows script for peoplegen
rem ---------------------------------------------------------------------------

set PEOPLEGEN_SCALA_OPTS=

rem Make sure Java user.home property accurately reflects home directory
if NOT "%HOME%"=="" set PEOPLEGEN_SCALA_OPTS=%PEOPLEGEN_SCALA_OPTS% -Duser.home="%HOME%"

if "%JAVA_HOME%" == "" set JAVA_HOME="@JAVA_HOME@"
%JAVA_HOME%\bin\java.exe -jar "@JAR@" %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end

:end
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal
