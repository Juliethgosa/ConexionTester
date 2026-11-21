# ConexionTester – Prueba de conexión JDBC con interfaz Swing

Aplicación de escritorio en Java (Swing) que permite escribir la configuración de una base de
datos (URL, usuario y contraseña), probar la conexión con JDBC y ver el resultado:
**mensaje verde** si conecta y **mensaje rojo** con el error técnico si falla.

Actividad EA3 – Unidad 3: Desarrollo de interfaces gráficas y conexión a bases de datos
(IU Digital de Antioquia).

## Requisitos
- JDK 17 o superior (`javac -version` debe mostrar 17 o más).
- El driver JDBC de SQLite ya viene en la carpeta `lib/` (`sqlite-jdbc-3.46.1.3.jar`, de Xerial,
  licencia Apache 2.0). Es el *driver* de la base de datos, no un framework: JDBC es la API estándar
  de Java y cada base de datos aporta su propio driver.

## Ejecutar
| Sistema | Abrir la ventana | Ejecutar las pruebas por consola |
|---|---|---|
| Windows (PowerShell) | `.\ejecutar.bat` | `.\probar.bat` |
| Linux / macOS | `./ejecutar.sh` | `./probar.sh` |

Los scripts compilan el código en la carpeta `out/` y lo ejecutan.

## Probar la ventana
| URL de conexión | Qué debe pasar |
|---|---|
| `jdbc:sqlite:inventario.db` | Mensaje verde: conexión exitosa (SQLite crea el archivo si no existe) |
| `jdbc:sqlite:/ruta/inexistente/inventario.db` | Mensaje rojo: fallo de conexión (`SQLException`) |
| `jdbc:mysql://localhost:3306/inventario` | Mensaje rojo: fallo del driver (`ClassNotFoundException`), porque el driver de MySQL no está en `lib/` |
| (vacía) | Mensaje rojo: entrada no válida |

## Estructura
```
src/co/edu/iudigital/conexion/
├── ConexionTester.java    JFrame + lógica de conexión (clase pedida por la actividad)
└── PruebasConexion.java   6 pruebas de la lógica, sin frameworks
lib/                       Driver JDBC de SQLite
docs/                      Informe en PDF y código fuente en CSV
```

## Conceptos aplicados
- **Swing:** `JFrame`, `JPanel` (`BorderLayout` y `GridLayout`), `JLabel`, `JTextField`,
  `JPasswordField`, `JButton`, `JTextArea` dentro de un `JScrollPane`.
- **Eventos:** `ActionListener` en el botón "Probar conexión".
- **JDBC:** `Class.forName` y `DriverManager.getConnection()`.
- **Excepciones:** `try-catch` con `ClassNotFoundException` y `SQLException` (se muestra `e.getMessage()`).
- **Recursos:** `try-with-resources` para cerrar la conexión siempre.
