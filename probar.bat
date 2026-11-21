@echo off
chcp 65001 >nul
rem Compila y ejecuta las pruebas de la logica de conexion (requiere JDK 17 o superior).
if exist out rmdir /s /q out
mkdir out
javac --release 17 -encoding UTF-8 -d out src\co\edu\iudigital\conexion\ConexionTester.java src\co\edu\iudigital\conexion\PruebasConexion.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "out;lib\*" co.edu.iudigital.conexion.PruebasConexion
