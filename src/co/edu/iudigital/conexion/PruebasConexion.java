package co.edu.iudigital.conexion;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pruebas de la lógica de conexión sin interfaz gráfica y sin frameworks externos.
 * Cada escenario compara el resultado obtenido con el esperado y muestra OK o FALLO.
 *
 * Uso: probar.bat  (Windows)  |  ./probar.sh  (Linux / macOS)
 */
public final class PruebasConexion {

    private record Escenario(String nombre, String url, Predicate<ConexionTester.Resultado> cumple, String esperado) { }

    private PruebasConexion() { }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        Path carpeta = Files.createTempDirectory("conexiontester");
        String archivoTemporal = carpeta.resolve("prueba.db").toString().replace('\\', '/');

        List<Escenario> escenarios = List.of(
                new Escenario("1. SQLite en memoria",
                        "jdbc:sqlite::memory:",
                        r -> r.exitoso() && r.mensaje().startsWith("CONEXIÓN EXITOSA"),
                        "CONEXIÓN EXITOSA"),
                new Escenario("2. SQLite en un archivo (se crea si no existe)",
                        "jdbc:sqlite:" + archivoTemporal,
                        r -> r.exitoso() && r.mensaje().startsWith("CONEXIÓN EXITOSA"),
                        "CONEXIÓN EXITOSA"),
                new Escenario("3. SQLite con una carpeta que no existe (SQLException)",
                        "jdbc:sqlite:/carpeta/que/no/existe/inventario.db",
                        r -> !r.exitoso() && r.mensaje().startsWith("FALLO DE CONEXIÓN"),
                        "FALLO DE CONEXIÓN"),
                new Escenario("4. MySQL sin el driver en el classpath (ClassNotFoundException)",
                        "jdbc:mysql://localhost:3306/inventario",
                        r -> !r.exitoso() && r.mensaje().startsWith("FALLO DEL DRIVER"),
                        "FALLO DEL DRIVER"),
                new Escenario("5. URL que ningún driver reconoce (SQLException)",
                        "abc",
                        r -> !r.exitoso() && r.mensaje().startsWith("FALLO DE CONEXIÓN"),
                        "FALLO DE CONEXIÓN"),
                new Escenario("6. URL vacía (validación de entrada)",
                        "   ",
                        r -> !r.exitoso() && r.mensaje().startsWith("ENTRADA NO VÁLIDA"),
                        "ENTRADA NO VÁLIDA"));

        System.out.println("=".repeat(78));
        System.out.println("PRUEBAS DE LA LÓGICA DE CONEXIÓN (ConexionTester.probarConexion)");
        System.out.println("=".repeat(78));

        int correctas = 0;
        for (Escenario e : escenarios) {
            ConexionTester.Resultado r = ConexionTester.probarConexion(e.url(), "", "");
            boolean ok = e.cumple().test(r);
            if (ok) {
                correctas++;
            }
            System.out.printf("%n[%s] %s%n", ok ? "OK" : "FALLO", e.nombre());
            System.out.println("      URL probada : \"" + e.url() + "\"");
            System.out.println("      Esperado    : " + e.esperado());
            System.out.println("      Obtenido    : " + primeraLinea(r.mensaje()));
            System.out.println("      Detalle     : " + tercerLinea(r.mensaje()));
        }

        System.out.printf("%nResultado: %d de %d pruebas correctas%n", correctas, escenarios.size());
    }

    private static String primeraLinea(String texto) {
        int salto = texto.indexOf('\n');
        return salto < 0 ? texto : texto.substring(0, salto);
    }

    /** El mensaje tiene el formato: título, línea en blanco, detalle. Devuelve el detalle. */
    private static String tercerLinea(String texto) {
        String[] lineas = texto.split("\n");
        return lineas.length > 2 ? lineas[2] : "";
    }
}
