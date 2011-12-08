@echo OFF
set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;lib\aislib-0.5.jar
set CLASSPATH=%CLASSPATH%;lib\log4j-1.2.15.jar
set CLASSPATH=%CLASSPATH%;lib\aisspy.jar
set CLASSPATH=%CLASSPATH%;lib\commons-lang-2.5.jar
set CLASSPATH=%CLASSPATH%;lib\commons-httpclient.jar
set CLASSPATH=%CLASSPATH%;lib\commons-logging.jar
set CLASSPATH=%CLASSPATH%;lib\commons-codec.jar
set CLASSPATH=%CLASSPATH%;lib\mail.jar

@echo ON
java -cp %CLASSPATH% dk.frv.aisspy.AisSpy
