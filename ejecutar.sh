#!/usr/bin/env bash
# Compila y abre la ventana de ConexionTester (requiere JDK 17 o superior).
set -e
rm -rf out && mkdir out
javac --release 17 -encoding UTF-8 -d out src/co/edu/iudigital/conexion/*.java
java -Dfile.encoding=UTF-8 -cp "out:lib/*" co.edu.iudigital.conexion.ConexionTester
