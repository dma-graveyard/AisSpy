@echo OFF
set CLASSPATH=.;lib/*

@echo ON
java -cp %CLASSPATH% dk.frv.aisspy.AisSpy
